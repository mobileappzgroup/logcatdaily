package dev.logcatdaily.samples.fileupload

import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The real UploadApi against a one-shot socket server: what goes on the wire.
// The unit-test classpath has no com.sun.net.httpserver, so the reply is typed out.
class UploadApiTest {

    private class Seen(var requestLine: String = "", val headers: MutableMap<String, String> = mutableMapOf(), var body: Int = 0)

    private fun serveOnce(reply: String, seen: Seen): ServerSocket {
        val server = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            server.accept().use { socket ->
                val input = socket.getInputStream()
                fun line(): String {
                    val out = StringBuilder()
                    while (true) {
                        val c = input.read()
                        if (c < 0 || c == '\n'.code) break
                        if (c != '\r'.code) out.append(c.toChar())
                    }
                    return out.toString()
                }
                seen.requestLine = line()
                while (true) {
                    val header = line()
                    if (header.isEmpty()) break
                    seen.headers[header.substringBefore(':').lowercase()] = header.substringAfter(':').trim()
                }
                val length = seen.headers["content-length"]?.toInt() ?: 0
                var left = length
                while (left > 0) left -= input.skip(left.toLong()).toInt().coerceAtLeast(1)
                seen.body = length
                socket.getOutputStream().write(reply.toByteArray())
            }
        }
        return server
    }

    @Test
    fun `the chunk call is a POST that says PATCH`() = runBlocking {
        val seen = Seen()
        val server = serveOnce("HTTP/1.1 204 No Content\r\nUpload-Offset: 1500\r\nConnection: close\r\n\r\n", seen)
        try {
            val api = UploadApi("http://127.0.0.1:${server.localPort}")
            val reply = api.sendChunk("/files/s-0001", 1000, ByteArray(500), 500)
            assertTrue(reply is ApiReply.Ok)
            assertEquals(1500L, (reply as ApiReply.Ok).offset)
            assertEquals("POST /files/s-0001 HTTP/1.1", seen.requestLine)
            assertEquals("PATCH", seen.headers["x-http-method-override"])
            assertEquals("1000", seen.headers["upload-offset"])
            assertEquals(500, seen.body)
        } finally {
            server.close()
        }
    }

    @Test
    fun `409 carries the server offset from the body`() = runBlocking {
        val seen = Seen()
        val body = "server offset is 4000\n"
        val server = serveOnce("HTTP/1.1 409 Conflict\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body", seen)
        try {
            val api = UploadApi("http://127.0.0.1:${server.localPort}")
            val reply = api.sendChunk("/files/s-0001", 3000, ByteArray(100), 100)
            assertEquals(4000L, (reply as ApiReply.Conflict).serverOffset)
        } finally {
            server.close()
        }
    }
}
