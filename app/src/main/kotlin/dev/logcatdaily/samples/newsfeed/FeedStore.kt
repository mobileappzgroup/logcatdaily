package dev.logcatdaily.samples.newsfeed

import androidx.room.withTransaction

// What the mediator needs from the database, so it runs on the plain JVM in tests.
interface FeedStore {
    suspend fun state(): FeedState?

    suspend fun postCount(): Int

    // Posts and cursor land together or not at all. refresh = true drops every
    // saved post and the old cursor first, in the same transaction.
    suspend fun applyPage(refresh: Boolean, posts: List<FeedPostEntity>, nextCursor: String?, now: Long)
}

class RoomFeedStore(private val db: FeedDatabase) : FeedStore {
    private val postDao = db.feedDao()
    private val stateDao = db.feedStateDao()

    override suspend fun state(): FeedState? = stateDao.get()

    override suspend fun postCount(): Int = postDao.count()

    override suspend fun applyPage(refresh: Boolean, posts: List<FeedPostEntity>, nextCursor: String?, now: Long) {
        db.withTransaction {
            val refreshedAt = if (refresh) now else stateDao.get()?.refreshedAt ?: now
            if (refresh) {
                postDao.clear()
                stateDao.clear()
            }
            postDao.insertAll(posts)
            stateDao.upsert(FeedState(nextCursor = nextCursor, endReached = nextCursor == null, refreshedAt = refreshedAt))
        }
    }
}
