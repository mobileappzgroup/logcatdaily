package dev.logcatdaily.samples.newsfeed

import android.content.Context
import androidx.room.Room

// Hand-wired, no DI library. One database and one api object for the cursor arm.
object FeedGraph {
    @Volatile
    private var repository: FeedRepository? = null

    fun repository(context: Context): FeedRepository =
        repository ?: synchronized(this) {
            repository ?: build(context.applicationContext).also { repository = it }
        }

    private fun build(context: Context): FeedRepository {
        // Sample data only: a schema change wipes the database.
        val db = Room.databaseBuilder(context, FeedDatabase::class.java, "feed.db")
            .fallbackToDestructiveMigration(true)
            .build()
        return FeedRepository(
            dao = db.feedDao(),
            mediator = FeedRemoteMediator(FeedApi(), RoomFeedStore(db)),
        )
    }
}
