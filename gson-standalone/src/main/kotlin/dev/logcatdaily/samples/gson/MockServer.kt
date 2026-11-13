package dev.logcatdaily.samples.gson

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

const val USERS_BODY = """[{"id":1,"name":"Nishant"},{"id":2,"name":"Logcat"}]"""

// A local server on a free port. GET /users answers with a JSON array.
fun startMockServer(): HttpServer {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/users") { ex ->
        val body = USERS_BODY.toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, body.size.toLong())
        ex.responseBody.use { it.write(body) }
    }
    server.start()
    return server
}
