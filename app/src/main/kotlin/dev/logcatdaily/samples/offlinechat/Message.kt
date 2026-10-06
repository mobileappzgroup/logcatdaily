package dev.logcatdaily.samples.offlinechat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageStatus { SENDING, SENT, FAILED }

const val DEFAULT_CONVERSATION_ID = "c1"
const val PHONE_SENDER_ID = "nishant"

// id is local only. clientId is not unique: if a send is retried without the
// Idempotency-Key, the server keeps two rows with the same clientId and sync
// stores both. serverSeq is the server's id for a row inside its conversation,
// unique once it is set. createdAt is the local tap time, sentAt the server's.
@Entity(
    tableName = "messages",
    indices = [Index("clientId"), Index("conversationId", "serverSeq", unique = true)],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val status: MessageStatus,
    val createdAt: Long,
    val sentAt: Long? = null,
    val serverSeq: Long? = null,
)

// One row per message that has not been acknowledged yet.
@Entity(tableName = "outbox")
data class OutboxEntry(
    @PrimaryKey val clientId: String,
    val attempts: Int = 0,
    val lastError: String? = null,
)

// Highest serverSeq a sync has returned for one conversation. Acks do not move
// it, so a row the server stored while its reply was lost is still fetched by
// the next sync.
@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val conversationId: String,
    val lastSeq: Long,
)
