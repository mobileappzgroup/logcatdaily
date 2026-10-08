package dev.logcatdaily.mockfeed

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Mock feed backend for the news-feed sample. JDK classes only. Mock data.
//
//   GET  /feed?limit=N                   the N newest posts (default 20, max 50)
//   GET  /feed?limit=N&after=cur_K       posts older than seq K, newest first
//   GET  /feed?limit=N&offset=M          posts at positions M.. of the current list
//
// Every reply is {"items":[...],"nextCursor":...} with one post per line.
//   POST /admin/reset                    back to the 100 seed posts, disarm everything
//   POST /admin/publish?n=2              n new posts at the top (p-0101, p-0102)
//   POST /admin/delete-head?n=2          delete the n newest posts
//   POST /admin/drop-next-reply          the next /feed request gets no reply
//   POST /admin/expire-next-cursor       the next /feed request with a cursor gets a 400
//   POST /admin/latency?ms=N             add N ms to every /feed reply

private const val PORT = 8081
private const val SEED_POSTS = 100
private const val DEFAULT_LIMIT = 20
private const val MAX_LIMIT = 50

private class Post(
    val seq: Int,
    val authorId: String,
    val author: String,
    val text: String,
    val createdAt: Long,
    var likeCount: Int,
) {
    val id get() = "p-%04d".format(seq)
}

private val authors = listOf("nishant", "asha", "dev", "meera", "kabir", "isha")

private val sentences = listOf(
    "Cut cold start by 300 ms after moving one init off the main thread.",
    "Room migration finally passed on the old test device.",
    "Why is my LazyColumn recomposing every row on scroll?",
    "Baseline profile shaved a full frame off the first list render.",
    "StrictMode caught a disk read on the main thread. Again.",
    "WorkManager ran my job twice. It was my fault.",
    "Moved a ViewModel to the right scope and the bug went away.",
    "Compose preview crashed, the app did not. Take the win.",
    "Retrofit or plain HttpURLConnection? I checked the APK size first.",
    "Flow collected in the wrong lifecycle again. Fixed with one line.",
)

private val lock = Any()
private val posts = mutableListOf<Post>()
private var nextSeq = 1
private var requestCount = 0
private val dropNextReply = AtomicBoolean(false)
private val expireNextCursor = AtomicBoolean(false)
private val latencyMs = AtomicLong(0)
private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun log(line: String) = println("${LocalTime.now().format(clock)}  $line")

private fun seed() {
    posts.clear()
    nextSeq = 1
    requestCount = 0
    val base = System.currentTimeMillis() - SEED_POSTS * 60_000L
    repeat(SEED_POSTS) { addPost(base + it * 60_000L) }
}

// seq is never reused, so a delete followed by a publish carries on from the
// highest seq ever handed out. Posts stay sorted newest first.
private fun addPost(createdAt: Long) {
    val seq = nextSeq++
    val authorIndex = (seq - 1) % authors.size
    val author = authors[authorIndex]
    posts.add(0, Post(seq, author, author, sentences[(seq - 1) % sentences.size], createdAt, (seq * 7) % 50 + 3))
}

fun main() {
    synchronized(lock) { seed() }

    val server = HttpServer.create(InetSocketAddress(PORT), 0)
    server.createContext("/feed") { exchange ->
        if (exchange.requestMethod != "GET") reply(exchange, 405, "{}") else handleFeed(exchange)
    }
    server.createContext("/admin/reset") { exchange ->
        synchronized(lock) { seed() }
        // Anything armed for one run must not leak into the next.
        dropNextReply.set(false)
        expireNextCursor.set(false)
        latencyMs.set(0)
        log("reset: $SEED_POSTS posts, p-0001..p-0100")
        reply(exchange, 200, """{"posts":$SEED_POSTS}""")
    }
    server.createContext("/admin/publish") { exchange ->
        val n = intParam(exchange, "n") ?: 1
        val ids = synchronized(lock) {
            repeat(n) { addPost(System.currentTimeMillis()) }
            posts.take(n).reversed().map { it.id }
        }
        log("published ${ids.joinToString(", ")}")
        reply(exchange, 200, """{"published":${ids.joinToString(",", "[", "]") { "\"$it\"" }}}""")
    }
    server.createContext("/admin/delete-head") { exchange ->
        val n = intParam(exchange, "n") ?: 1
        val ids = synchronized(lock) {
            val gone = posts.take(n).map { it.id }
            repeat(gone.size) { posts.removeAt(0) }
            gone
        }
        log("deleted ${ids.joinToString(", ")}")
        reply(exchange, 200, """{"deleted":${ids.joinToString(",", "[", "]") { "\"$it\"" }}}""")
    }
    server.createContext("/admin/drop-next-reply") { exchange ->
        dropNextReply.set(true)
        log("armed: the next /feed request gets no reply")
        reply(exchange, 200, """{"drop":true}""")
    }
    server.createContext("/admin/expire-next-cursor") { exchange ->
        expireNextCursor.set(true)
        log("armed: the next /feed request with a cursor gets a 400")
        reply(exchange, 200, """{"expire":true}""")
    }
    server.createContext("/admin/latency") { exchange ->
        val ms = (intParam(exchange, "ms") ?: 0).coerceAtLeast(0)
        latencyMs.set(ms.toLong())
        log("latency: ${ms} ms on every /feed reply")
        reply(exchange, 200, """{"ms":$ms}""")
    }
    server.start()
    log("mock-feed-server listening on :$PORT, ${posts.size} posts")
}

private fun handleFeed(exchange: HttpExchange) {
    val query = queryOf(exchange)
    val limit = (query["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
    val after = query["after"]
    val offset = query["offset"]?.toIntOrNull()?.coerceAtLeast(0)
    val shape = buildString {
        append("limit=$limit")
        if (after != null) append(" after=$after")
        if (offset != null) append(" offset=$offset")
    }

    if (dropNextReply.getAndSet(false)) {
        log("GET /feed $shape -> reply dropped")
        // Closing before any status line goes out: the client sees the
        // connection end with no answer.
        exchange.close()
        return
    }
    if (after != null && expireNextCursor.getAndSet(false)) {
        log("GET /feed $shape -> 400 cursor expired")
        reply(exchange, 400, """{"error":"cursor expired"}""")
        return
    }
    val afterSeq = after?.let { parseCursor(it) }
    if (after != null && afterSeq == null) {
        log("GET /feed $shape -> 400 bad cursor")
        reply(exchange, 400, """{"error":"bad cursor"}""")
        return
    }

    val page: List<Post>
    val nextCursor: String?
    synchronized(lock) {
        requestCount++
        page = when {
            afterSeq != null -> posts.filter { it.seq < afterSeq }.take(limit)
            offset != null -> posts.drop(offset).take(limit)
            else -> posts.take(limit)
        }
        // Offset replies never carry a cursor. A cursor page has one while
        // older posts remain.
        val last = page.lastOrNull()
        nextCursor = if (offset == null && last != null && posts.any { it.seq < last.seq }) "cur_${last.seq}" else null
        // Mock data: a count moves whenever a post is served, so a cached count
        // goes stale. Same rule every time: posts whose seq matches the request
        // number mod 3 gain a like.
        page.filter { it.seq % 3 == requestCount % 3 }.forEach { it.likeCount++ }
    }

    val range = if (page.isEmpty()) "(none)" else "${page.first().id}..${page.last().id}"
    val next = if (offset == null) ", next=${nextCursor ?: "null"}" else ""
    log("GET /feed $shape -> $range$next")

    val delay = latencyMs.get()
    if (delay > 0) Thread.sleep(delay)
    reply(exchange, 200, feedJson(page, nextCursor))
}

// "cur_81" means older than seq 81. The client treats it as opaque.
private fun parseCursor(cursor: String): Int? =
    Regex("^cur_(\\d{1,9})$").find(cursor)?.groupValues?.get(1)?.toInt()

// One post per line inside items, so a curl from the Mac reads like a table.
private fun feedJson(page: List<Post>, nextCursor: String?): String {
    val items = if (page.isEmpty()) "[]" else page.joinToString(",\n", "[\n", "\n]") {
        """{"id":"${it.id}","seq":${it.seq},"authorId":"${it.authorId}","author":"${it.author}",""" +
            """"text":"${escape(it.text)}","createdAt":${it.createdAt},"likeCount":${it.likeCount}}"""
    }
    val cursor = if (nextCursor == null) "null" else "\"$nextCursor\""
    return """{"items":$items,"nextCursor":$cursor}"""
}

private fun queryOf(exchange: HttpExchange): Map<String, String> =
    (exchange.requestURI.rawQuery ?: "").split("&").filter { it.contains("=") }.associate {
        val (key, value) = it.split("=", limit = 2)
        key to java.net.URLDecoder.decode(value, "UTF-8")
    }

private fun intParam(exchange: HttpExchange, name: String): Int? = queryOf(exchange)[name]?.toIntOrNull()

private fun reply(exchange: HttpExchange, code: Int, json: String) {
    val bytes = (json + "\n").toByteArray()
    exchange.responseHeaders.add("Content-Type", "application/json")
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}

private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
