package dev.logcatdaily.samples.newsfeed

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FeedPostEntity::class, FeedState::class], version = 1, exportSchema = false)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun feedStateDao(): FeedStateDao
}
