package dev.logcatdaily.samples.offlinechat

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class, OutboxEntry::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun outboxDao(): OutboxDao
}
