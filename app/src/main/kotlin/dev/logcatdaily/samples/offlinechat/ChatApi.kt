package dev.logcatdaily.samples.offlinechat

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// The server said no and will keep saying no (4xx). Retrying is pointless.
class ServerRejectedException(val code: Int) : Exception("HTTP $code")

class ServerMessage(val serverSeq: Long, val clientId: String, val text: String)

// Plain HttpURLConnection against the mock on the Mac. 10.0.2.2 is the
// emulator's alias for the host machine.
class ChatApi(private val baseUrl: String = "http://10.0.2.2:8080") {

    // Debug toggle for the video: false drops the Idempotency-Key header.
    @Volatile
    var sendIdempotencyKey = true

    // Returns serverSeq. Network trouble and 5xx throw IOException (retry later),
    // a 4xx throws ServerRejectedException.
    suspend fun send(clientId: String, text: String): Long = withContext(Dispatchers.IO) {
        val body = JSONObject().put("clientId", clientId).put("text", text).toString().toByteArray()
        val connection = URL("$baseUrl/messages").openConnection() as HttpURLConnection
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
                        JSONObject(reply).getLong("serverSeq")
                    } catch (e: JSONException) {
                        throw IOException("bad reply: $reply", e)
                    }
                }
                code in 400..499 -> throw ServerRejectedException(code)
                else -> throw IOException("HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    // Every row the server holds with a serverSeq above `after`, oldest first.
    suspend fun fetchAfter(after: Long): List<ServerMessage> = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl/messages?after=$after").openConnection() as HttpURLConnection
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
                    ServerMessage(row.getLong("seq"), row.getString("clientId"), row.getString("text"))
                }
            } catch (e: JSONException) {
                throw IOException("bad reply: $reply", e)
            }
        } finally {
            connection.disconnect()
        }
    }
}
