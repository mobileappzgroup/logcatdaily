package dev.logcatdaily.samples.offlinechat

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// In-memory stand-ins for the Room DAOs, enough for the repository's queries.
class FakeMessageDao : MessageDao {
    val rows = mutableListOf<Message>()
    val syncState = mutableMapOf<String, Long>()
    private var nextId = 1L

    override fun observeAll(conversationId: String): Flow<List<Message>> =
        flowOf(
            rows.filter { it.conversationId == conversationId }
                .sortedWith(compareBy({ it.serverSeq == null }, { it.serverSeq }, { it.createdAt }, { it.id })),
        )

    override suspend fun getUnacked(clientId: String): Message? =
        rows.firstOrNull { it.clientId == clientId && it.serverSeq == null }

    override suspend fun countBySeq(conversationId: String, serverSeq: Long): Int =
        rows.count { it.conversationId == conversationId && it.serverSeq == serverSeq }

    override suspend fun insert(message: Message) {
        val taken = message.serverSeq != null &&
            rows.any { it.conversationId == message.conversationId && it.serverSeq == message.serverSeq }
        if (!taken) rows += message.copy(id = nextId++)
    }

    override suspend fun markSent(id: Long, serverSeq: Long, sentAt: Long) =
        update(id) { it.serverSeq == null }.let { index ->
            if (index >= 0) rows[index] = rows[index].copy(status = MessageStatus.SENT, serverSeq = serverSeq, sentAt = sentAt)
        }

    override suspend fun markFailed(id: Long) =
        update(id) { it.serverSeq == null }.let { index ->
            if (index >= 0) rows[index] = rows[index].copy(status = MessageStatus.FAILED)
        }

    override suspend fun markSending(id: Long): Int {
        val index = update(id) { it.status == MessageStatus.FAILED && it.serverSeq == null }
        if (index < 0) return 0
        rows[index] = rows[index].copy(status = MessageStatus.SENDING)
        return 1
    }

    override suspend fun lastSyncedSeq(conversationId: String): Long? = syncState[conversationId]

    override suspend fun saveSyncState(state: SyncState) {
        syncState[state.conversationId] = state.lastSeq
    }

    private fun update(id: Long, matches: (Message) -> Boolean): Int =
        rows.indexOfFirst { it.id == id && matches(it) }
}

class FakeOutboxDao(private val messages: FakeMessageDao) : OutboxDao {
    val entries = mutableMapOf<String, OutboxEntry>()

    override suspend fun insert(entry: OutboxEntry) {
        entries[entry.clientId] = entry
    }

    override suspend fun upsert(entry: OutboxEntry) {
        entries[entry.clientId] = entry
    }

    override suspend fun get(clientId: String): OutboxEntry? = entries[clientId]

    override suspend fun delete(clientId: String) {
        entries.remove(clientId)
    }

    override suspend fun recordFailure(clientId: String, error: String) {
        entries[clientId]?.let { entries[clientId] = it.copy(attempts = it.attempts + 1, lastError = error) }
    }

    override suspend fun pendingClientIds(conversationId: String): List<String> =
        messages.rows
            .filter {
                it.conversationId == conversationId && it.status == MessageStatus.SENDING &&
                    it.serverSeq == null && it.clientId in entries
            }
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
            .map { it.clientId }

    override suspend fun nextClientId(conversationId: String): String? =
        pendingClientIds(conversationId).firstOrNull()
}

object DirectTransactor : Transactor {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

// A tiny server behind the ChatApi seam. `failures` are used up one per send,
// oldest first; a null entry means the send goes through. `lostAcks` makes the
// server store the message and then fail the call, like a dropped reply.
// `gate`, when set, holds the next send on the wire until it is completed.
class FakeApi : ChatApi() {
    val stored = mutableListOf<ServerMessage>()
    val failures = ArrayDeque<Exception?>()
    var lostAcks = 0
    var sendCalls = 0
    var gate: CompletableDeferred<Unit>? = null

    // While true every send fails before it reaches the server, like airplane mode.
    var offline = false
    private val acks = mutableMapOf<String, SendAck>()
    private var clock = 1_000L

    override suspend fun send(conversationId: String, clientId: String, senderId: String, text: String): SendAck {
        sendCalls++
        gate?.let {
            gate = null
            it.await()
        }
        if (offline) throw IOException("offline")
        failures.removeFirstOrNull()?.let { throw it }
        val first = if (sendIdempotencyKey) acks[clientId] else null
        val ack = first ?: SendAck(stored.size + 1L, clock++).also {
            stored += ServerMessage(it.serverSeq, clientId, senderId, text, it.sentAt)
            if (sendIdempotencyKey) acks[clientId] = it
        }
        if (lostAcks > 0) {
            lostAcks--
            throw IOException("reply lost")
        }
        return ack
    }

    override suspend fun fetchAfter(conversationId: String, after: Long): List<ServerMessage> =
        stored.filter { it.serverSeq > after }

    fun otherPhoneSends(clientId: String, text: String) {
        stored += ServerMessage(stored.size + 1L, clientId, "friend", text, clock++)
    }
}
