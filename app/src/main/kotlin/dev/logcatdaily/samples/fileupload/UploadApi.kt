package dev.logcatdaily.samples.fileupload

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// What the server said. Network trouble is not here: it is thrown as IOException.
sealed interface ApiReply {
    class Ok(val code: Int, val offset: Long?, val location: String?) : ApiReply
    // 409: the offset in the header is not the server's. serverOffset comes from the body.
    class Conflict(val serverOffset: Long?) : ApiReply
    // 404 or 410: the server does not know the session.
    class Gone(val code: Int) : ApiReply
    // 408, 425, 429, 5xx: try again later.
    class ServerError(val code: Int) : ApiReply
    // Any other 4xx, 422 included.
    class Rejected(val code: Int) : ApiReply
}

// Plain HttpURLConnection against the mock on the Mac. 10.0.2.2 is the
// emulator's alias for the host machine. The shape is tus-like (HEAD for the
// offset, a chunk call that appends at an offset), not tus.
open class UploadApi(private val baseUrl: String = "http://10.0.2.2:8083") {

    open suspend fun createSession(totalBytes: Long, sha256: String, idempotencyKey: String?): ApiReply =
        call("/files", "POST") { c ->
            c.setRequestProperty("Upload-Length", totalBytes.toString())
            c.setRequestProperty("X-Content-SHA256", sha256)
            if (idempotencyKey != null)
                c.setRequestProperty("Idempotency-Key", idempotencyKey)
            c.doOutput = true
            c.setFixedLengthStreamingMode(0)
            c.outputStream.close()
        }

    open suspend fun queryOffset(sessionUrl: String): ApiReply = call(sessionUrl, "HEAD") { }

    // The design says PATCH. HttpURLConnection is not documented to accept it,
    // so the sample sends POST with the override header the server also accepts.
    open suspend fun sendChunk(sessionUrl: String, offset: Long, data: ByteArray, length: Int): ApiReply =
        call(sessionUrl, "POST") { c ->
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            c.setRequestProperty("Upload-Offset", offset.toString())
            c.setRequestProperty("Content-Type", "application/offset+octet-stream")
            c.doOutput = true
            c.setFixedLengthStreamingMode(length)
            c.outputStream.use { it.write(data, 0, length) }
        }

    open suspend fun deleteSession(sessionUrl: String): ApiReply = call(sessionUrl, "DELETE") { }

    // The single variant: the whole file in one request, nothing remembered.
    // onSent gets the bytes handed to the socket so far, about once per MiB.
    open suspend fun uploadWhole(file: File, onSent: (Long) -> Unit = {}): ApiReply =
        call("/upload", "POST") { c ->
            c.doOutput = true
            c.setFixedLengthStreamingMode(file.length())
            file.inputStream().use { input ->
                c.outputStream.use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var sent = 0L
                    var reported = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        sent += n
                        if (sent - reported >= 1_048_576) {
                            reported = sent
                            onSent(sent)
                        }
                    }
                }
            }
        }

    // The blocking write and read run on IO inside suspendCancellableCoroutine.
    // Cancelling the coroutine does not unblock a socket read by itself, so
    // invokeOnCancellation closes the connection from the cancelling thread.
    private suspend fun call(path: String, method: String, send: (HttpURLConnection) -> Unit): ApiReply =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
                cont.invokeOnCancellation { connection.disconnect() }
                try {
                    connection.requestMethod = method
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    send(connection)
                    cont.resume(read(connection))
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun read(c: HttpURLConnection): ApiReply {
        val code = c.responseCode
        val body = if (code in 200..299) "" else runCatching { c.errorStream?.use { it.readBytes().decodeToString() } }.getOrNull().orEmpty()
        return when {
            code in 200..299 -> ApiReply.Ok(code, c.getHeaderField("Upload-Offset")?.toLongOrNull(), c.getHeaderField("Location"))
            code == 409 -> ApiReply.Conflict(body.trim().substringAfterLast(' ').toLongOrNull())
            code == 404 || code == 410 -> ApiReply.Gone(code)
            code == 408 || code == 425 || code == 429 || code in 500..599 -> ApiReply.ServerError(code)
            else -> ApiReply.Rejected(code)
        }
    }
}
