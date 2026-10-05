package dev.logcatdaily.samples.offlinechat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY createdAt")
    fun observeAll(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE clientId = :clientId")
    suspend fun get(clientId: String): Message?

    @Insert
    suspend fun insert(message: Message)

    @Query("UPDATE messages SET status = 'SENT', serverSeq = :serverSeq WHERE clientId = :clientId")
    suspend fun markSent(clientId: String, serverSeq: Long)

    @Query("UPDATE messages SET status = 'FAILED' WHERE clientId = :clientId")
    suspend fun markFailed(clientId: String)
}
