package dev.logcatdaily.samples.newsfeed

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FeedDao {

    // The screen reads only this. Room invalidates the PagingSource whenever
    // feed_posts changes.
    @Query("SELECT * FROM feed_posts ORDER BY seq DESC")
    fun pagingSource(): PagingSource<Int, FeedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<FeedPostEntity>)

    @Query("SELECT COUNT(*) FROM feed_posts")
    suspend fun count(): Int

    @Query("DELETE FROM feed_posts")
    suspend fun clear()
}

@Dao
interface FeedStateDao {

    @Query("SELECT * FROM feed_state WHERE feedId = '$HOME_FEED_ID'")
    suspend fun get(): FeedState?

    @Upsert
    suspend fun upsert(state: FeedState)

    @Query("DELETE FROM feed_state")
    suspend fun clear()
}
