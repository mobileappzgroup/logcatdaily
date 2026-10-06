package dev.logcatdaily.samples.offlinechat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(entry: OutboxEntry)

    @Upsert
    suspend fun upsert(entry: OutboxEntry)

    @Query("SELECT * FROM outbox WHERE clientId = :clientId")
    suspend fun get(clientId: String): OutboxEntry?

    @Query("DELETE FROM outbox WHERE clientId = :clientId")
    suspend fun delete(clientId: String)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE clientId = :clientId")
    suspend fun recordFailure(clientId: String, error: String)

    // The queue for one conversation: messages still waiting to go out, oldest
    // tap first (id breaks a tie in the same millisecond).
    @Query(
        "SELECT outbox.clientId FROM outbox " +
            "JOIN messages ON messages.clientId = outbox.clientId " +
            "WHERE messages.conversationId = :conversationId " +
            "AND messages.status = 'SENDING' AND messages.serverSeq IS NULL " +
            "ORDER BY messages.createdAt, messages.id"
    )
    suspend fun pendingClientIds(conversationId: String): List<String>

    // The head of that queue.
    @Query(
        "SELECT outbox.clientId FROM outbox " +
            "JOIN messages ON messages.clientId = outbox.clientId " +
            "WHERE messages.conversationId = :conversationId " +
            "AND messages.status = 'SENDING' AND messages.serverSeq IS NULL " +
            "ORDER BY messages.createdAt, messages.id LIMIT 1"
    )
    suspend fun nextClientId(conversationId: String): String?
}
