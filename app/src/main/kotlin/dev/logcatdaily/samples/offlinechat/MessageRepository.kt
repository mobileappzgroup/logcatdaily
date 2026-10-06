package dev.logcatdaily.samples.offlinechat

import android.util.Log
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

private enum class MergeResult { NEW, ADOPTED, SKIPPED }

class MessageRepository(
    private val messages: MessageDao,
    private val outbox: OutboxDao,
    private val api: ChatApi,
    private val transactor: Transactor,
    // Queues the unique SendWorker request for a clientId.
    private val scheduleRetry: (String) -> Unit,
    private val conversationId: String = DEFAULT_CONVERSATION_ID,
    private val senderId: String = PHONE_SENDER_ID,
) {
    // Not tied to the screen: leaving the chat must not cancel a send that is
    // already on the wire.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val syncLock = Mutex()

    val observeMessages: Flow<List<Message>> = messages.observeAll(conversationId)

    var sendWithoutKey: Boolean
        get() = !api.sendIdempotencyKey
        set(value) {
            api.sendIdempotencyKey = !value
        }

    suspend fun send(text: String) {
        val clientId = UUID.randomUUID().toString()
        // Message and outbox row go in together or not at all. A message
        // without an outbox row would sit in SENDING forever.
        transactor.run {
            messages.insert(
                Message(
                    clientId = clientId,
                    conversationId = conversationId,
                    senderId = senderId,
                    text = text,
                    status = MessageStatus.SENDING,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            outbox.insert(OutboxEntry(clientId))
        }
        scope.launch {
            if (attemptSend(clientId) == SendOutcome.RETRY) scheduleRetry(clientId)
        }
    }

    // Tap on a FAILED bubble: start over with a fresh attempt count.
    suspend fun retry(clientId: String) {
        val message = messages.getUnacked(clientId) ?: return
        val reset = transactor.run {
            if (messages.markSending(message.id) == 0) return@run false
            outbox.upsert(OutboxEntry(clientId))
            true
        }
        if (!reset) return
        Log.d(TAG, "retry clientId=${clientId.take(8)}")
        scheduleRetry(clientId)
    }

    // Called when the chat opens, in case the process died after the insert.
    suspend fun resumePending() {
        val unsent = outbox.pendingClientIds()
        Log.d(TAG, "resumePending found ${unsent.size} unsent")
        unsent.forEach { scheduleRetry(it) }
    }

    suspend fun attemptSend(clientId: String): SendOutcome {
        val message = messages.getUnacked(clientId)
        if (message == null || message.status != MessageStatus.SENDING) return SendOutcome.DONE

        Log.d(TAG, "send attempt clientId=${clientId.take(8)} key=${api.sendIdempotencyKey}")
        return try {
            val ack = api.send(conversationId, clientId, senderId, message.text)
            transactor.run {
                messages.markSent(message.id, ack.serverSeq, ack.sentAt)
                outbox.delete(clientId)
            }
            Log.d(TAG, "acked clientId=${clientId.take(8)} serverSeq=${ack.serverSeq}")
            sync()
            SendOutcome.DONE
        } catch (e: ServerRejectedException) {
            giveUp(message, e.message)
        } catch (e: IOException) {
            outbox.recordFailure(clientId, e.message ?: e.javaClass.simpleName)
            Log.d(TAG, "send failed clientId=${clientId.take(8)} error=${e.message}")
            // No outbox row left: a sync or a concurrent ack already finished this message.
            val attempts = outbox.get(clientId)?.attempts ?: return SendOutcome.DONE
            if (attempts >= MAX_ATTEMPTS) giveUp(message, e.message) else SendOutcome.RETRY
        }
    }

    private suspend fun giveUp(message: Message, error: String?): SendOutcome {
        transactor.run {
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
            val rows = api.fetchAfter(conversationId, messages.lastSyncedSeq(conversationId) ?: 0L)
            if (rows.isEmpty()) return@withLock
            val upTo = rows.maxOf { it.serverSeq }
            val results = transactor.run {
                val merged = rows.map { merge(it) }
                messages.saveSyncState(SyncState(conversationId, upTo))
                merged
            }
            Log.d(
                TAG,
                "sync fetched ${rows.size}: ${results.count { it == MergeResult.NEW }} new, " +
                    "${results.count { it == MergeResult.ADOPTED }} adopted, " +
                    "${results.count { it == MergeResult.SKIPPED }} skipped, up to seq=$upTo",
            )
        } catch (e: IOException) {
            Log.d(TAG, "sync skipped error=${e.message}")
        }
    }

    private suspend fun merge(row: ServerMessage): MergeResult {
        // Already stored, usually because the ack carried this seq.
        if (messages.countBySeq(conversationId, row.serverSeq) > 0) return MergeResult.SKIPPED
        val pending = messages.getUnacked(row.clientId)
        if (pending != null) {
            // Our own send, stored by the server but never acked here.
            messages.markSent(pending.id, row.serverSeq, row.sentAt)
            outbox.delete(row.clientId)
            return MergeResult.ADOPTED
        } else {
            messages.insert(
                Message(
                    clientId = row.clientId,
                    conversationId = conversationId,
                    senderId = row.senderId,
                    text = row.text,
                    status = MessageStatus.SENT,
                    createdAt = row.sentAt,
                    sentAt = row.sentAt,
                    serverSeq = row.serverSeq,
                ),
            )
            return MergeResult.NEW
        }
    }
}
