package dev.logcatdaily.samples.newsfeed

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

// The server answered 400 to a page request that carried a cursor: the cursor
// is bad or expired. Retrying the same cursor is pointless, the feed starts over.
class BadCursorException(val cursor: String) : Exception("HTTP 400 for $cursor")

class FeedPage(val items: List<FeedPost>, val nextCursor: String?)

// Plain HttpURLConnection against the mock on the Mac. 10.0.2.2 is the
// emulator's alias for the host machine. Network trouble and any non-2xx throw
// IOException; a 400 on a cursor page throws BadCursorException.
open class FeedApi(private val baseUrl: String = "http://10.0.2.2:8081") {

    // One page by cursor. A null cursor is the first page.
    open suspend fun page(cursor: String?, limit: Int): FeedPage {
        val after = cursor?.let { "&after=" + URLEncoder.encode(it, "UTF-8") } ?: ""
        return get("limit=$limit$after", cursor)
    }

    // One page by position, for the offset arm. nextCursor comes back null.
    open suspend fun pageByOffset(offset: Int, limit: Int): FeedPage = get("limit=$limit&offset=$offset", null)

    private suspend fun get(query: String, cursor: String?): FeedPage = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl/feed?$query").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            val code = connection.responseCode
            if (code == 400 && cursor != null) throw BadCursorException(cursor)
            if (code !in 200..299) throw IOException("HTTP $code")
            val reply = connection.inputStream.use { it.readBytes().decodeToString() }
            try {
                parse(reply)
            } catch (e: JSONException) {
                throw IOException("bad reply: $reply", e)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(reply: String): FeedPage {
        val json = JSONObject(reply)
        val array = json.getJSONArray("items")
        val items = List(array.length()) {
            val row = array.getJSONObject(it)
            FeedPost(
                id = row.getString("id"),
                seq = row.getInt("seq"),
                authorId = row.getString("authorId"),
                author = row.getString("author"),
                text = row.getString("text"),
                createdAt = row.getLong("createdAt"),
                likeCount = row.getInt("likeCount"),
            )
        }
        val next = if (json.isNull("nextCursor")) null else json.getString("nextCursor")
        return FeedPage(items, next)
    }
}
