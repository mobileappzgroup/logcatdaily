package dev.logcatdaily.samples.offlinechat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(entry: OutboxEntry)

    @Query("SELECT * FROM outbox WHERE clientId = :clientId")
    suspend fun get(clientId: String): OutboxEntry?

    @Query("DELETE FROM outbox WHERE clientId = :clientId")
    suspend fun delete(clientId: String)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE clientId = :clientId")
    suspend fun recordFailure(clientId: String, error: String)

    // Oldest first, and only messages that are still waiting to go out.
    @Query(
        "SELECT outbox.clientId FROM outbox " +
            "JOIN messages ON messages.clientId = outbox.clientId " +
            "WHERE messages.status = 'SENDING' AND messages.serverSeq IS NULL ORDER BY messages.createdAt"
    )
    suspend fun pendingClientIds(): List<String>
}
