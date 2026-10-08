package dev.logcatdaily.samples.newsfeed

// The server as a plain list, newest first, same rules as mock-feed-server.
class FakeFeedServer(seedPosts: Int = 100) {
    val posts = mutableListOf<FeedPost>()
    private var nextSeq = 1

    init {
        repeat(seedPosts) { addPost() }
    }

    private fun addPost() {
        val seq = nextSeq++
        posts.add(0, FeedPost("p-%04d".format(seq), seq, "nishant", "nishant", "post $seq", seq * 1_000L, 0))
    }

    fun publish(n: Int) = repeat(n) { addPost() }

    fun deleteHead(n: Int) = repeat(n) { posts.removeAt(0) }

    // Older than the cursor, newest first. The cursor stays null on the last page.
    fun byCursor(cursor: String?, limit: Int): FeedPage {
        val afterSeq = cursor?.let { parseCursor(it) }
        val page = posts.filter { afterSeq == null || it.seq < afterSeq }.take(limit)
        val last = page.lastOrNull()
        val next = if (last != null && posts.any { it.seq < last.seq }) "cur_${last.seq}" else null
        return FeedPage(page, next)
    }

    fun byOffset(offset: Int, limit: Int) = FeedPage(posts.drop(offset).take(limit), null)

    private fun parseCursor(cursor: String): Int =
        cursor.removePrefix("cur_").toIntOrNull() ?: throw BadCursorException(cursor)
}

// A FeedApi that answers from FakeFeedServer. `failures` are used up one per
// call, oldest first; a null entry means the call goes through.
class FakeFeedApi(val server: FakeFeedServer = FakeFeedServer()) : FeedApi() {
    val failures = ArrayDeque<Exception?>()

    // Every call as the query it would have been: "after=cur_81", "first", "offset=20".
    val calls = mutableListOf<String>()

    override suspend fun page(cursor: String?, limit: Int): FeedPage {
        calls += if (cursor == null) "first" else "after=$cursor"
        failures.removeFirstOrNull()?.let { throw it }
        return server.byCursor(cursor, limit)
    }

    override suspend fun pageByOffset(offset: Int, limit: Int): FeedPage {
        calls += "offset=$offset"
        failures.removeFirstOrNull()?.let { throw it }
        return server.byOffset(offset, limit)
    }
}

// In-memory stand-in for Room. applyPage is all or nothing, like the transaction.
class FakeFeedStore : FeedStore {
    val posts = mutableListOf<FeedPostEntity>()
    var saved: FeedState? = null
    var applyCalls = 0

    override suspend fun state(): FeedState? = saved

    override suspend fun postCount(): Int = posts.size

    override suspend fun applyPage(refresh: Boolean, posts: List<FeedPostEntity>, nextCursor: String?, now: Long) {
        applyCalls++
        val refreshedAt = if (refresh) now else saved?.refreshedAt ?: now
        if (refresh) this.posts.clear()
        this.posts += posts
        saved = FeedState(nextCursor = nextCursor, endReached = nextCursor == null, refreshedAt = refreshedAt)
    }

    // Newest first, like the DAO query.
    fun ids() = posts.sortedByDescending { it.seq }.map { it.id }
}
