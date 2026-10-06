package dev.logcatdaily.mockchat

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

// Mock chat backend for the offline-chat sample. JDK classes only.
//
//   POST /conversations/{id}/messages          store a message, dedupe on Idempotency-Key
//   GET  /conversations/{id}/messages?after=N  rows with seq above N (default 0)
//
// Any conversation id works, each one counts its own seq from 1.
//   POST /admin/drop-next-reply  the next stored message gets no reply (lost ack)
//   POST /admin/reset            forget everything
//
// Start with --drop-next-reply (or DROP_NEXT_REPLY=1) to arm the drop at launch.

private const val PORT = 8080

private class Row(val seq: Int, val clientId: String, val senderId: String, val text: String, val sentAt: Long)

private val lock = Any()
private val conversations = mutableMapOf<String, MutableList<Row>>()
private val replyByKey = mutableMapOf<String, Row>()
private val dropNextReply = AtomicBoolean(false)
private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun log(line: String) = println("${LocalTime.now().format(clock)}  $line")

private fun short(id: String) = id.take(8)

fun main(args: Array<String>) {
    dropNextReply.set("--drop-next-reply" in args || System.getenv("DROP_NEXT_REPLY") == "1")

    val server = HttpServer.create(InetSocketAddress(PORT), 0)
    server.createContext("/conversations/") { exchange ->
        val conversationId = Regex("^/conversations/([^/]+)/messages$").find(exchange.requestURI.path)?.groupValues?.get(1)
        when {
            conversationId == null -> reply(exchange, 404, "{}")
            exchange.requestMethod == "POST" -> handlePost(exchange, conversationId)
            exchange.requestMethod == "GET" -> handleList(exchange, conversationId)
            else -> reply(exchange, 405, "{}")
        }
    }
    server.createContext("/admin/drop-next-reply") { exchange ->
        dropNextReply.set(true)
        log("armed: the next stored message will get no reply")
        reply(exchange, 200, """{"drop":true}""")
    }
    server.createContext("/admin/reset") { exchange ->
        synchronized(lock) {
            conversations.clear()
            replyByKey.clear()
        }
        // A drop armed for one run must not leak into the next.
        dropNextReply.set(false)
        log("reset: 0 rows")
        reply(exchange, 200, """{"rows":0}""")
    }
    server.start()
    log("mock-chat-server listening on :$PORT drop-next-reply=${dropNextReply.get()}")
}

private fun handlePost(exchange: HttpExchange, conversationId: String) {
    val body = exchange.requestBody.readBytes().decodeToString()
    val clientId = field(body, "clientId")
    val senderId = field(body, "senderId")
    val text = field(body, "text")
    if (clientId == null || senderId == null || text == null) {
        reply(exchange, 400, """{"error":"clientId, senderId and text are required"}""")
        return
    }
    val key = exchange.requestHeaders.getFirst("Idempotency-Key")
    val scopedKey = key?.let { "$conversationId/$it" }

    val stored: Row
    synchronized(lock) {
        val first = scopedKey?.let { replyByKey[it] }
        if (first != null) {
            log("duplicate key clientId=${short(first.clientId)} returned seq=${first.seq}")
            reply(exchange, 200, replyJson(first))
            return
        }
        val rows = conversations.getOrPut(conversationId) { mutableListOf() }
        stored = Row(rows.size + 1, clientId, senderId, text, System.currentTimeMillis())
        rows += stored
        if (scopedKey != null) replyByKey[scopedKey] = stored
    }
    val keyNote = if (key == null) " (no Idempotency-Key)" else ""
    log("stored clientId=${short(stored.clientId)} seq=${stored.seq}$keyNote text=\"${stored.text}\"")

    if (dropNextReply.getAndSet(false)) {
        log("reply dropped for clientId=${short(stored.clientId)} seq=${stored.seq}")
        // Closing before any status line goes out: the client sees the
        // connection end with no answer, the row stays stored.
        exchange.close()
        return
    }
    reply(exchange, 200, replyJson(stored))
}

// One row per line, so a curl from the Mac reads like a table.
private fun handleList(exchange: HttpExchange, conversationId: String) {
    val after = Regex("after=(\\d+)").find(exchange.requestURI.query ?: "")?.groupValues?.get(1)?.toInt() ?: 0
    val json = synchronized(lock) {
        val rows = conversations[conversationId].orEmpty().filter { it.seq > after }
        if (rows.isEmpty()) "[]" else rows.joinToString(",\n", "[\n", "\n]") {
            """{"seq":${it.seq},"clientId":"${it.clientId}","senderId":"${escape(it.senderId)}",""" +
                """"text":"${escape(it.text)}","sentAt":${it.sentAt}}"""
        }
    }
    reply(exchange, 200, json)
}

private fun replyJson(row: Row) = """{"clientId":"${row.clientId}","serverSeq":${row.seq},"sentAt":${row.sentAt}}"""

private fun reply(exchange: HttpExchange, code: Int, json: String) {
    val bytes = (json + "\n").toByteArray()
    exchange.responseHeaders.add("Content-Type", "application/json")
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}

// Enough JSON for the two string fields this server reads.
private fun field(json: String, name: String): String? {
    val match = Regex("\"$name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json) ?: return null
    return unescape(match.groupValues[1])
}

private fun unescape(s: String) = Regex("\\\\(u[0-9a-fA-F]{4}|.)").replace(s) {
    val code = it.groupValues[1]
    when {
        code.length == 5 -> code.substring(1).toInt(16).toChar().toString()
        code == "n" -> "\n"
        code == "t" -> "\t"
        else -> code
    }
}

private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
