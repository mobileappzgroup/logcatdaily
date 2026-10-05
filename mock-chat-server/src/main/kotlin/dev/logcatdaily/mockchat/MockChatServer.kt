package dev.logcatdaily.mockchat

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

// Mock chat backend for the offline-chat sample. JDK classes only.
//
//   POST /messages               store a message, dedupe on Idempotency-Key
//   GET  /messages               list what is stored
//   POST /admin/drop-next-reply  the next stored message gets no reply (lost ack)
//   POST /admin/reset            forget everything
//
// Start with --drop-next-reply (or DROP_NEXT_REPLY=1) to arm the drop at launch.

private const val PORT = 8080

private class Row(val seq: Int, val clientId: String, val text: String)

private val lock = Any()
private val rows = mutableListOf<Row>()
private val replyByKey = mutableMapOf<String, Row>()
private val dropNextReply = AtomicBoolean(false)
private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun log(line: String) = println("${LocalTime.now().format(clock)}  $line")

private fun short(id: String) = id.take(8)

fun main(args: Array<String>) {
    dropNextReply.set("--drop-next-reply" in args || System.getenv("DROP_NEXT_REPLY") == "1")

    val server = HttpServer.create(InetSocketAddress(PORT), 0)
    server.createContext("/messages") { exchange ->
        when (exchange.requestMethod) {
            "POST" -> handlePost(exchange)
            "GET" -> handleList(exchange)
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
            rows.clear()
            replyByKey.clear()
        }
        log("reset: 0 rows")
        reply(exchange, 200, """{"rows":0}""")
    }
    server.start()
    log("mock-chat-server listening on :$PORT drop-next-reply=${dropNextReply.get()}")
}

private fun handlePost(exchange: HttpExchange) {
    val body = exchange.requestBody.readBytes().decodeToString()
    val clientId = field(body, "clientId")
    val text = field(body, "text")
    if (clientId == null || text == null) {
        reply(exchange, 400, """{"error":"clientId and text are required"}""")
        return
    }
    val key = exchange.requestHeaders.getFirst("Idempotency-Key")

    val stored: Row
    synchronized(lock) {
        val first = key?.let { replyByKey[it] }
        if (first != null) {
            log("duplicate key clientId=${short(first.clientId)} returned seq=${first.seq}")
            reply(exchange, 200, replyJson(first))
            return
        }
        stored = Row(rows.size + 1, clientId, text)
        rows += stored
        if (key != null) replyByKey[key] = stored
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

private fun handleList(exchange: HttpExchange) {
    val json = synchronized(lock) {
        rows.joinToString(",", "[", "]") {
            """{"seq":${it.seq},"clientId":"${it.clientId}","text":"${escape(it.text)}"}"""
        }
    }
    reply(exchange, 200, json)
}

private fun replyJson(row: Row) = """{"clientId":"${row.clientId}","serverSeq":${row.seq}}"""

private fun reply(exchange: HttpExchange, code: Int, json: String) {
    val bytes = json.toByteArray()
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
