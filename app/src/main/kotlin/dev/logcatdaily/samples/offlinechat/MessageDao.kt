package dev.logcatdaily.samples.offlinechat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // Acked rows in the server's order, then sends still waiting, oldest first.
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY serverSeq IS NULL, serverSeq, createdAt, id")
    fun observeAll(conversationId: String): Flow<List<Message>>

    // The local row for a send that has no serverSeq yet.
    @Query("SELECT * FROM messages WHERE clientId = :clientId AND serverSeq IS NULL LIMIT 1")
    suspend fun getUnacked(clientId: String): Message?

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND serverSeq = :serverSeq")
    suspend fun countBySeq(conversationId: String, serverSeq: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message)

    @Query("UPDATE messages SET status = 'SENT', serverSeq = :serverSeq, sentAt = :sentAt WHERE id = :id AND serverSeq IS NULL")
    suspend fun markSent(id: Long, serverSeq: Long, sentAt: Long)

    @Query("UPDATE messages SET status = 'FAILED' WHERE id = :id AND serverSeq IS NULL")
    suspend fun markFailed(id: Long)

    // Tap on a FAILED bubble: back to waiting.
    @Query("UPDATE messages SET status = 'SENDING' WHERE id = :id AND status = 'FAILED' AND serverSeq IS NULL")
    suspend fun markSending(id: Long): Int

    @Query("SELECT lastSeq FROM sync_state WHERE conversationId = :conversationId")
    suspend fun lastSyncedSeq(conversationId: String): Long?

    @Upsert
    suspend fun saveSyncState(state: SyncState)
}
