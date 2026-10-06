package dev.logcatdaily.samples.offlinechat

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(entities = [Message::class, OutboxEntry::class, SyncState::class], version = 2, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun outboxDao(): OutboxDao
}

// Lets the repository run its multi-step writes without knowing about Room.
interface Transactor {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactor(private val db: ChatDatabase) : Transactor {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}
