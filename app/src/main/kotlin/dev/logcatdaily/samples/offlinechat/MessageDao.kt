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
    @Query("SELECT * FROM messages ORDER BY serverSeq IS NULL, serverSeq, createdAt, id")
    fun observeAll(): Flow<List<Message>>

    // The local row for a send that has no serverSeq yet.
    @Query("SELECT * FROM messages WHERE clientId = :clientId AND serverSeq IS NULL LIMIT 1")
    suspend fun getUnacked(clientId: String): Message?

    @Query("SELECT COUNT(*) FROM messages WHERE serverSeq = :serverSeq")
    suspend fun countBySeq(serverSeq: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message)

    @Query("UPDATE messages SET status = 'SENT', serverSeq = :serverSeq WHERE id = :id AND serverSeq IS NULL")
    suspend fun markSent(id: Long, serverSeq: Long)

    @Query("UPDATE messages SET status = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: Long)

    @Query("SELECT lastSeq FROM sync_state WHERE id = 0")
    suspend fun lastSyncedSeq(): Long?

    @Upsert
    suspend fun saveSyncState(state: SyncState)
}
