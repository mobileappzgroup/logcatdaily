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
            messages.insert(Message(clientId, text, MessageStatus.SENDING, System.currentTimeMillis()))
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
        val message = messages.get(clientId)
        if (message == null || message.status != MessageStatus.SENDING) return SendOutcome.DONE

        Log.d(TAG, "send attempt clientId=${clientId.take(8)} key=${api.sendIdempotencyKey}")
        return try {
            val serverSeq = api.send(clientId, message.text)
            db.withTransaction {
                messages.markSent(clientId, serverSeq)
                outbox.delete(clientId)
            }
            Log.d(TAG, "acked clientId=${clientId.take(8)} serverSeq=$serverSeq")
            SendOutcome.DONE
        } catch (e: ServerRejectedException) {
            giveUp(clientId, e.message)
        } catch (e: IOException) {
            outbox.recordFailure(clientId, e.message ?: e.javaClass.simpleName)
            Log.d(TAG, "send failed clientId=${clientId.take(8)} error=${e.message}")
            val attempts = outbox.get(clientId)?.attempts ?: MAX_ATTEMPTS
            if (attempts >= MAX_ATTEMPTS) giveUp(clientId, e.message) else SendOutcome.RETRY
        }
    }

    private suspend fun giveUp(clientId: String, error: String?): SendOutcome {
        db.withTransaction {
            messages.markFailed(clientId)
            outbox.recordFailure(clientId, error ?: "unknown")
        }
        Log.d(TAG, "gave up clientId=${clientId.take(8)} error=$error")
        return SendOutcome.GAVE_UP
    }

    private fun enqueueRetry(clientId: String) {
        workManager.enqueueUniqueWork(
            SendWorker.uniqueName(clientId),
            ExistingWorkPolicy.KEEP,
            SendWorker.request(clientId),
        )
    }
}
