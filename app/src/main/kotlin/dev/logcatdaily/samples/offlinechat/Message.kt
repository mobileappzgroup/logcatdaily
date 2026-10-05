package dev.logcatdaily.samples.offlinechat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageStatus { SENDING, SENT, FAILED }

// id is local only. clientId is not unique: if a send is retried without the
// Idempotency-Key, the server keeps two rows with the same clientId and sync
// stores both. serverSeq is the server's id for a row, unique once it is set.
@Entity(
    tableName = "messages",
    indices = [Index("clientId"), Index("serverSeq", unique = true)],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val text: String,
    val status: MessageStatus,
    val createdAt: Long,
    val serverSeq: Long? = null,
)

// One row per message that has not been acknowledged yet.
@Entity(tableName = "outbox")
data class OutboxEntry(
    @PrimaryKey val clientId: String,
    val attempts: Int = 0,
    val lastError: String? = null,
)

// Highest serverSeq a sync has returned. Acks do not move it, so a row the
// server stored while its reply was lost is still fetched by the next sync.
@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val id: Int = 0,
    val lastSeq: Long,
)
