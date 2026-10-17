package dev.logcatdaily.mockupload

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Mock upload backend for the file-upload sample. JDK classes only. Mock data.
// Shaped like the core of the tus protocol (HEAD for the offset, a chunk call
// that appends at an offset, 409 on a mismatch). Not compliant with it.
//
//   POST   /files                    new session; needs Upload-Length. Idempotency-Key repeats get 200
//   HEAD   /files/{id}               200 with Upload-Offset and Upload-Length
//   PATCH  /files/{id}               append a chunk at Upload-Offset (also POST + X-HTTP-Method-Override: PATCH)
//   DELETE /files/{id}               forget the session, remove the file
//   POST   /upload                   the whole file in one request
//
//   POST /admin/reset                clear sessions, counters and every armed knob
//   POST /admin/throttle?kbps=N      body read rate, default 700
//   POST /admin/drop-reply-at?offset=N   the next chunk call at that offset is applied, then no reply
//   POST /admin/drop-create-reply    the next POST /files is stored, its reply is dropped
//   GET  /admin/stats                counters
//   GET  /admin/files                one line per session
//
// Start with --keep-partial to keep the bytes of a chunk that broke mid-body.

private const val PORT = 8083
private const val DEFAULT_KBPS = 700L
private const val STEP = 64 * 1024
private const val MIB = 1024.0 * 1024.0

private class Session(val id: String, val length: Long, val key: String?, val sha256: String?, val file: File) {
    var offset = 0L
    var complete = false
    var overrideLogged = false
}

private val lock = Any()
private val sessions = LinkedHashMap<String, Session>()
private val keys = HashMap<String, String>()
private var nextSession = 1
private var nextWhole = 1
private var conflicts = 0
private var completed = 0
private val bytesReceived = AtomicLong(0)
private val throttleKbps = AtomicLong(DEFAULT_KBPS)
private val dropReplyAt = AtomicLong(-1)
private val dropCreateReply = AtomicBoolean(false)
private var keepPartial = false
private val dir = File("mock-upload-server/build/uploads")
private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun log(line: String) = println("${LocalTime.now().format(clock)}  $line")

fun main(args: Array<String>) {
    keepPartial = "--keep-partial" in args
    reset()

    val server = HttpServer.create(InetSocketAddress(PORT), 0)
    server.createContext("/files") { exchange ->
        try {
            handleFiles(exchange)
        } catch (e: IOException) {
            // The client went away while we were answering. Nothing to clean up.
            log("${exchange.requestMethod} ${exchange.requestURI.path} -> connection lost (${e.javaClass.simpleName})")
            exchange.close()
        }
    }
    server.createContext("/upload") { exchange ->
        if (exchange.requestMethod != "POST") reply(exchange, 405, "") else handleWhole(exchange)
    }
    server.createContext("/admin") { exchange -> handleAdmin(exchange) }
    // A throttled body must not hold up HEAD or the admin calls.
    server.executor = Executors.newCachedThreadPool()
    server.start()
    log("mock-upload-server listening on :$PORT, throttle $DEFAULT_KBPS KB/s" + if (keepPartial) ", keep-partial" else "")
}

private fun reset() {
    synchronized(lock) {
        sessions.clear()
        keys.clear()
        nextSession = 1
        nextWhole = 1
        conflicts = 0
        completed = 0
        bytesReceived.set(0)
        // Anything armed for one run must not leak into the next.
        throttleKbps.set(DEFAULT_KBPS)
        dropReplyAt.set(-1)
        dropCreateReply.set(false)
        dir.deleteRecursively()
        dir.mkdirs()
    }
}

private fun handleFiles(exchange: HttpExchange) {
    val path = exchange.requestURI.path
    val id = path.removePrefix("/files").removePrefix("/").ifEmpty { null }
    val override = exchange.requestHeaders.getFirst("X-HTTP-Method-Override")
    val method = if (exchange.requestMethod == "POST" && id != null && override.equals("PATCH", true)) "PATCH" else exchange.requestMethod
    when {
        id == null && method == "POST" -> handleCreate(exchange)
        id != null && method == "HEAD" -> handleHead(exchange, id)
        id != null && method == "PATCH" -> handleChunk(exchange, id, override != null)
        id != null && method == "DELETE" -> handleDelete(exchange, id)
        else -> reply(exchange, 405, "")
    }
}

private fun handleCreate(exchange: HttpExchange) {
    exchange.requestBody.use { it.readBytes() }
    val length = exchange.requestHeaders.getFirst("Upload-Length")?.toLongOrNull()
    if (length == null || length <= 0) {
        log("POST /files -> 400 Upload-Length missing")
        reply(exchange, 400, "Upload-Length is required")
        return
    }
    val key = exchange.requestHeaders.getFirst("Idempotency-Key")
    val sha = exchange.requestHeaders.getFirst("X-Content-SHA256")
    val tag = key?.take(4) ?: "none"
    val session: Session
    val repeated: Boolean
    synchronized(lock) {
        val known = key?.let { keys[it] }?.let { sessions[it] }
        repeated = known != null
        session = known ?: run {
            val id = "s-%04d".format(nextSession++)
            Session(id, length, key, sha, File(dir, "$id.bin")).also {
                sessions[id] = it
                if (key != null) keys[key] = id
            }
        }
    }
    val line = if (repeated) "POST /files key=$tag -> 200 ${session.id} (same key)" else "POST /files length=$length key=$tag -> 201 ${session.id}"
    if (dropCreateReply.getAndSet(false)) {
        log("$line, reply dropped")
        exchange.close()
        return
    }
    log(line)
    exchange.responseHeaders.add("Location", "/files/${session.id}")
    reply(exchange, if (repeated) 200 else 201, "")
}

private fun handleHead(exchange: HttpExchange, id: String) {
    val session = synchronized(lock) { sessions[id] }
    if (session == null) {
        log("HEAD /files/$id -> 404")
        exchange.sendResponseHeaders(404, -1)
        exchange.close()
        return
    }
    val offset = synchronized(lock) { session.offset }
    log("HEAD /files/$id -> offset $offset")
    exchange.responseHeaders.add("Upload-Offset", offset.toString())
    exchange.responseHeaders.add("Upload-Length", session.length.toString())
    exchange.responseHeaders.add("Cache-Control", "no-store")
    exchange.sendResponseHeaders(200, -1)
    exchange.close()
}

private fun handleChunk(exchange: HttpExchange, id: String, viaOverride: Boolean) {
    val session = synchronized(lock) { sessions[id] }
    if (session == null) {
        exchange.requestBody.use { it.readBytes() }
        log("PATCH /files/$id -> 404")
        reply(exchange, 404, "no such session")
        return
    }
    val offset = exchange.requestHeaders.getFirst("Upload-Offset")?.toLongOrNull()
    val expected = exchange.requestHeaders.getFirst("Content-Length")?.toLongOrNull() ?: -1L
    if (offset == null || expected < 0) {
        exchange.requestBody.use { it.readBytes() }
        log("PATCH /files/$id -> 400 Upload-Offset or Content-Length missing")
        reply(exchange, 400, "Upload-Offset and Content-Length are required")
        return
    }
    synchronized(lock) {
        if (viaOverride && !session.overrideLogged) {
            session.overrideLogged = true
            log("method override PATCH")
        }
    }
    val current = synchronized(lock) { session.offset }
    if (offset != current) {
        // Drain the body first so the client is not left writing into a closed socket.
        exchange.requestBody.use { it.readBytes() }
        synchronized(lock) { conflicts++ }
        log("PATCH /files/$id offset=$offset -> 409 server offset is $current")
        reply(exchange, 409, "server offset is $current")
        return
    }

    val body = readThrottled(exchange, expected)
    bytesReceived.addAndGet(body.size.toLong())
    if (body.size.toLong() < expected) {
        // The client closed mid-body.
        if (keepPartial && body.isNotEmpty()) {
            synchronized(lock) {
                session.file.appendBytes(body)
                session.offset += body.size
            }
            log("PATCH /files/$id offset=$offset len=$expected -> aborted, kept ${body.size} bytes, offset=${session.offset}")
        } else {
            log("PATCH /files/$id offset=$offset len=$expected -> aborted, chunk discarded")
        }
        exchange.close()
        return
    }

    var verdict = 204
    var newOffset: Long
    synchronized(lock) {
        if (sessions[id] !== session || session.offset != offset) {
            // The session was deleted or moved while the body was arriving.
            verdict = 409
            newOffset = session.offset
            conflicts++
        } else {
            session.file.appendBytes(body)
            session.offset += body.size
            newOffset = session.offset
            if (session.offset == session.length) {
                if (session.sha256 != null && fileSha256(session.file) != session.sha256) {
                    verdict = 422
                    sessions.remove(id)
                    session.file.delete()
                } else {
                    session.complete = true
                    completed++
                }
            }
        }
    }
    when (verdict) {
        409 -> {
            log("PATCH /files/$id offset=$offset -> 409 server offset is $newOffset")
            reply(exchange, 409, "server offset is $newOffset")
        }
        422 -> {
            log("PATCH /files/$id offset=$offset len=$expected -> 422 sha256 differs, session dropped")
            reply(exchange, 422, "sha256 differs")
        }
        else -> {
            if (dropReplyAt.compareAndSet(offset, -1)) {
                log("PATCH /files/$id offset=$offset len=$expected -> applied, reply dropped")
                // Closing before any status line goes out: the client sees the
                // connection end with no answer.
                exchange.close()
                return
            }
            val done = if (session.complete) " complete" else ""
            log("PATCH /files/$id offset=$offset len=$expected -> 204 offset=$newOffset$done")
            exchange.responseHeaders.add("Upload-Offset", newOffset.toString())
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
    }
}

private fun handleDelete(exchange: HttpExchange, id: String) {
    val session = synchronized(lock) { sessions.remove(id)?.also { s -> s.key?.let { keys.remove(it) } } }
    if (session == null) {
        log("DELETE /files/$id -> 404")
        exchange.sendResponseHeaders(404, -1)
        exchange.close()
        return
    }
    session.file.delete()
    log("DELETE /files/$id -> 204")
    exchange.sendResponseHeaders(204, -1)
    exchange.close()
}

private fun handleWhole(exchange: HttpExchange) {
    val expected = exchange.requestHeaders.getFirst("Content-Length")?.toLongOrNull() ?: -1L
    if (expected < 0) {
        log("POST /upload -> 411 Content-Length missing")
        reply(exchange, 411, "Content-Length is required")
        return
    }
    val body = readThrottled(exchange, expected)
    bytesReceived.addAndGet(body.size.toLong())
    val received = "received %.1f of %.1f MB".format(body.size / MIB, expected / MIB)
    if (body.size.toLong() < expected) {
        log("POST /upload -> aborted, $received")
        exchange.close()
        return
    }
    val n = synchronized(lock) { nextWhole++ }
    File(dir, "upload-%04d.bin".format(n)).writeBytes(body)
    log("POST /upload -> 201, $received")
    reply(exchange, 201, "stored")
}

// Reads up to `expected` bytes in 64 KB steps and sleeps so the average rate is
// the throttle. A client that closes early leaves a shorter array.
private fun readThrottled(exchange: HttpExchange, expected: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream(expected.coerceAtMost(4L * 1024 * 1024).toInt())
    val buffer = ByteArray(STEP)
    var left = expected
    try {
        exchange.requestBody.use { input ->
            while (left > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (n < 0) break
                out.write(buffer, 0, n)
                left -= n
                val kbps = throttleKbps.get().coerceAtLeast(1)
                Thread.sleep(n * 1000L / (kbps * 1024L))
            }
        }
    } catch (e: IOException) {
        // Reset by the peer: keep what arrived.
    }
    return out.toByteArray()
}

private fun handleAdmin(exchange: HttpExchange) {
    when (exchange.requestURI.path) {
        "/admin/reset" -> {
            reset()
            log("reset")
            reply(exchange, 200, """{"reset":true}""")
        }
        "/admin/throttle" -> {
            val kbps = (queryOf(exchange)["kbps"]?.toLongOrNull() ?: DEFAULT_KBPS).coerceAtLeast(1)
            throttleKbps.set(kbps)
            log("throttle: $kbps KB/s")
            reply(exchange, 200, """{"kbps":$kbps}""")
        }
        "/admin/drop-reply-at" -> {
            val offset = queryOf(exchange)["offset"]?.toLongOrNull()
            if (offset == null) {
                reply(exchange, 400, """{"error":"offset is required"}""")
                return
            }
            dropReplyAt.set(offset)
            log("armed: the chunk call at offset=$offset is applied, then no reply")
            reply(exchange, 200, """{"offset":$offset}""")
        }
        "/admin/drop-create-reply" -> {
            dropCreateReply.set(true)
            log("armed: the next POST /files is stored, its reply is dropped")
            reply(exchange, 200, """{"drop":true}""")
        }
        "/admin/stats" -> {
            val json = synchronized(lock) {
                """{"sessions":$nextSessionCount,"bytesReceived":${bytesReceived.get()},"conflicts":$conflicts,"completed":$completed}"""
            }
            reply(exchange, 200, json)
        }
        "/admin/files" -> {
            val lines = synchronized(lock) {
                sessions.values.joinToString("\n") {
                    """{"id":"${it.id}","size":${it.length},"offset":${it.offset},"complete":${it.complete},"sha256":"${it.sha256 ?: ""}"}"""
                }
            }
            reply(exchange, 200, lines)
        }
        else -> reply(exchange, 404, "")
    }
}

// Sessions that were created, not the ones still alive: a delete does not lower it.
private val nextSessionCount get() = nextSession - 1

private fun fileSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(STEP)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun queryOf(exchange: HttpExchange): Map<String, String> =
    (exchange.requestURI.rawQuery ?: "").split("&").filter { it.contains("=") }.associate {
        val (key, value) = it.split("=", limit = 2)
        key to java.net.URLDecoder.decode(value, "UTF-8")
    }

private fun reply(exchange: HttpExchange, code: Int, text: String) {
    val bytes = (if (text.isEmpty()) "" else text + "\n").toByteArray()
    exchange.responseHeaders.add("Content-Type", "text/plain")
    if (bytes.isEmpty()) {
        exchange.sendResponseHeaders(code, -1)
    } else {
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
    exchange.close()
}
