package dev.logcatdaily.samples.offlinechat

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus { SENDING, SENT, FAILED }

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val clientId: String,
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
