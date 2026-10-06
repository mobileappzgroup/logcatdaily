package dev.logcatdaily.samples.offlinechat

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// The server said no and will keep saying no (a 4xx that is not 408, 425 or 429).
// Retrying is pointless.
class ServerRejectedException(val code: Int) : Exception("HTTP $code")

class SendAck(val serverSeq: Long, val sentAt: Long)

class ServerMessage(
    val serverSeq: Long,
    val clientId: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
)

// Timeouts and rate limits (408, 425, 429) and 5xx are worth another try. Any
// other 4xx is final. Other non-2xx codes are treated as trouble on the way.
fun failureFor(code: Int): Exception = when {
    code == 408 || code == 425 || code == 429 -> IOException("HTTP $code")
    code in 400..499 -> ServerRejectedException(code)
    else -> IOException("HTTP $code")
}

// Plain HttpURLConnection against the mock on the Mac. 10.0.2.2 is the
// emulator's alias for the host machine.
open class ChatApi(private val baseUrl: String = "http://10.0.2.2:8080") {

    // Debug toggle for the video: false drops the Idempotency-Key header.
    @Volatile
    var sendIdempotencyKey = true

    // Network trouble, 5xx, 408, 425 and 429 throw IOException (retry later),
    // any other 4xx throws ServerRejectedException.
    open suspend fun send(
        conversationId: String,
        clientId: String,
        senderId: String,
        text: String,
    ): SendAck = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("clientId", clientId)
            .put("senderId", senderId)
            .put("text", text)
            .toString()
            .toByteArray()
        val connection = URL("$baseUrl/conversations/$conversationId/messages").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.doOutput = true
            // Streaming mode also stops HttpURLConnection from quietly
            // re-sending the POST by itself when the reply never arrives.
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Content-Type", "application/json")
            if (sendIdempotencyKey) {
                connection.setRequestProperty("Idempotency-Key", clientId)
            }
            connection.outputStream.use { it.write(body) }

            val code = connection.responseCode
            when {
                code in 200..299 -> {
                    val reply = connection.inputStream.use { it.readBytes().decodeToString() }
                    try {
                        val json = JSONObject(reply)
                        SendAck(json.getLong("serverSeq"), json.getLong("sentAt"))
                    } catch (e: JSONException) {
                        throw IOException("bad reply: $reply", e)
                    }
                }
                else -> throw failureFor(code)
            }
        } finally {
            connection.disconnect()
        }
    }

    // Every row of the conversation with a serverSeq above `after`, oldest first.
    open suspend fun fetchAfter(conversationId: String, after: Long): List<ServerMessage> = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl/conversations/$conversationId/messages?after=$after").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val reply = connection.inputStream.use { it.readBytes().decodeToString() }
            try {
                val array = JSONArray(reply)
                List(array.length()) {
                    val row = array.getJSONObject(it)
                    ServerMessage(
                        row.getLong("seq"),
                        row.getString("clientId"),
                        row.getString("senderId"),
                        row.getString("text"),
                        row.getLong("sentAt"),
                    )
                }
            } catch (e: JSONException) {
                throw IOException("bad reply: $reply", e)
            }
        } finally {
            connection.disconnect()
        }
    }
}
