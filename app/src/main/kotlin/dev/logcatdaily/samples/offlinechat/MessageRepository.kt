package dev.logcatdaily.samples.offlinechat

import android.util.Log
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "logcatdaily"
private const val MAX_ATTEMPTS = 8

enum class SendOutcome { DONE, RETRY, GAVE_UP }

class MessageRepository(
    private val db: ChatDatabase,
    private val api: ChatApi,
    private val workManager: WorkManager,
) {
    private val messages = db.messageDao()
    private val outbox = db.outboxDao()

    // Not tied to the screen: leaving the chat must not cancel a send that is
    // already on the wire.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val syncLock = Mutex()

    val observeMessages: Flow<List<Message>> = messages.observeAll()

    var sendWithoutKey: Boolean
        get() = !api.sendIdempotencyKey
        set(value) {
            api.sendIdempotencyKey = !value
        }

    suspend fun send(text: String) {
        val clientId = UUID.randomUUID().toString()
        // Message and outbox row go in together or not at all. A message
        // without an outbox row would sit in SENDING forever.
        db.withTransaction {
            messages.insert(Message(clientId = clientId, text = text, status = MessageStatus.SENDING, createdAt = System.currentTimeMillis()))
            outbox.insert(OutboxEntry(clientId))
        }
        scope.launch {
            if (attemptSend(clientId) == SendOutcome.RETRY) enqueueRetry(clientId)
        }
    }

    // Called when the chat opens, in case the process died after the insert.
    suspend fun resumePending() {
        outbox.pendingClientIds().forEach { enqueueRetry(it) }
    }

    suspend fun attemptSend(clientId: String): SendOutcome {
        val message = messages.getUnacked(clientId)
        if (message == null || message.status != MessageStatus.SENDING) return SendOutcome.DONE

        Log.d(TAG, "send attempt clientId=${clientId.take(8)} key=${api.sendIdempotencyKey}")
        return try {
            val serverSeq = api.send(clientId, message.text)
            db.withTransaction {
                messages.markSent(message.id, serverSeq)
                outbox.delete(clientId)
            }
            Log.d(TAG, "acked clientId=${clientId.take(8)} serverSeq=$serverSeq")
            sync()
            SendOutcome.DONE
        } catch (e: ServerRejectedException) {
            giveUp(message, e.message)
        } catch (e: IOException) {
            outbox.recordFailure(clientId, e.message ?: e.javaClass.simpleName)
            Log.d(TAG, "send failed clientId=${clientId.take(8)} error=${e.message}")
            val attempts = outbox.get(clientId)?.attempts ?: MAX_ATTEMPTS
            if (attempts >= MAX_ATTEMPTS) giveUp(message, e.message) else SendOutcome.RETRY
        }
    }

    private suspend fun giveUp(message: Message, error: String?): SendOutcome {
        db.withTransaction {
            messages.markFailed(message.id)
            outbox.recordFailure(message.clientId, error ?: "unknown")
        }
        Log.d(TAG, "gave up clientId=${message.clientId.take(8)} error=$error")
        return SendOutcome.GAVE_UP
    }

    // Pulls every server row after the last one a sync returned and merges it
    // by serverSeq, the server's own id. Runs on chat open and after each ack.
    suspend fun sync() = syncLock.withLock {
        try {
            val rows = api.fetchAfter(messages.lastSyncedSeq() ?: 0L)
            if (rows.isEmpty()) return@withLock
            db.withTransaction {
                rows.forEach { merge(it) }
                messages.saveSyncState(SyncState(lastSeq = rows.maxOf { it.serverSeq }))
            }
            Log.d(TAG, "sync merged ${rows.size} server rows, up to seq=${rows.maxOf { it.serverSeq }}")
        } catch (e: IOException) {
            Log.d(TAG, "sync skipped error=${e.message}")
        }
    }

    private suspend fun merge(row: ServerMessage) {
        // Already stored, usually because the ack carried this seq.
        if (messages.countBySeq(row.serverSeq) > 0) return
        val pending = messages.getUnacked(row.clientId)
        if (pending != null) {
            // Our own send, stored by the server but never acked here.
            messages.markSent(pending.id, row.serverSeq)
            outbox.delete(row.clientId)
        } else {
            messages.insert(
                Message(
                    clientId = row.clientId,
                    text = row.text,
                    status = MessageStatus.SENT,
                    createdAt = System.currentTimeMillis(),
                    serverSeq = row.serverSeq,
                ),
            )
        }
    }

    private fun enqueueRetry(clientId: String) {
        workManager.enqueueUniqueWork(
            SendWorker.uniqueName(clientId),
            ExistingWorkPolicy.KEEP,
            SendWorker.request(clientId),
        )
    }
}
