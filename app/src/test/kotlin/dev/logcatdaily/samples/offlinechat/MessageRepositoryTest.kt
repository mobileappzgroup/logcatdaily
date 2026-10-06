package dev.logcatdaily.samples.offlinechat

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRepositoryTest {

    private val messages = FakeMessageDao()
    private val outbox = FakeOutboxDao(messages)
    private val api = FakeApi()
    private val scheduled = mutableListOf<String>()

    // Unconfined: the foreground drain that send() launches runs inside the test.
    private val repository = MessageRepository(
        messages, outbox, api, DirectTransactor, { scheduled += it },
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
    private var tapClock = 10L

    // A message the way send() leaves it: a SENDING row and an outbox row.
    private fun pending(clientId: String, text: String = "hi") = runBlocking {
        messages.insert(
            Message(
                clientId = clientId,
                conversationId = DEFAULT_CONVERSATION_ID,
                senderId = PHONE_SENDER_ID,
                text = text,
                status = MessageStatus.SENDING,
                createdAt = tapClock++,
            ),
        )
        outbox.insert(OutboxEntry(clientId))
    }

    private fun row(clientId: String) = messages.rows.single { it.clientId == clientId }

    // merge, seen through sync()

    @Test
    fun skipsARowThatIsAlreadyStored() = runBlocking {
        pending("a")
        repository.drain()
        val before = messages.rows.toList()

        repository.sync()

        assertEquals(before, messages.rows)
        assertEquals(1L, messages.syncState[DEFAULT_CONVERSATION_ID])
    }

    @Test
    fun adoptsTheServerRowForAnUnackedSend() = runBlocking {
        pending("a")
        // The server stored it, the reply never made it back.
        api.stored += ServerMessage(1, "a", PHONE_SENDER_ID, "hi", 500)

        repository.sync()

        val adopted = row("a")
        assertEquals(MessageStatus.SENT, adopted.status)
        assertEquals(1L, adopted.serverSeq)
        assertEquals(500L, adopted.sentAt)
        assertNull(outbox.get("a"))
    }

    @Test
    fun newRowFromTheOtherPhoneBecomesItsOwnSentMessage() = runBlocking {
        api.otherPhoneSends("f1", "hey")

        repository.sync()

        val added = row("f1")
        assertEquals(MessageStatus.SENT, added.status)
        assertEquals("friend", added.senderId)
        assertEquals(1L, added.serverSeq)
        assertEquals(added.sentAt, added.createdAt)
    }

    @Test
    fun syncOnlyAsksForRowsAfterTheCursor() = runBlocking {
        api.otherPhoneSends("f1", "one")
        repository.sync()
        api.otherPhoneSends("f2", "two")

        repository.sync()

        assertEquals(listOf(1L, 2L), messages.rows.map { it.serverSeq })
        assertEquals(2L, messages.syncState[DEFAULT_CONVERSATION_ID])
    }

    // drain, one message

    @Test
    fun successMarksTheMessageSent() = runBlocking {
        pending("a")

        assertEquals(DrainResult.DONE, repository.drain())

        val sent = row("a")
        assertEquals(MessageStatus.SENT, sent.status)
        assertEquals(1L, sent.serverSeq)
        assertTrue(sent.sentAt != null)
        assertNull(outbox.get("a"))
        assertEquals(1, api.stored.size)
    }

    @Test
    fun networkErrorRetriesAndCountsTheAttempt() = runBlocking {
        pending("a")
        api.failures += IOException("offline")

        assertEquals(DrainResult.RETRY, repository.drain())

        assertEquals(MessageStatus.SENDING, row("a").status)
        assertEquals(1, outbox.get("a")?.attempts)
    }

    @Test
    fun rateLimitedRetries() = runBlocking {
        pending("a")
        api.failures += failureFor(429)

        assertEquals(DrainResult.RETRY, repository.drain())
        assertEquals(MessageStatus.SENDING, row("a").status)
    }

    @Test
    fun badRequestIsFinal() = runBlocking {
        pending("a")
        api.failures += failureFor(400)

        assertEquals(DrainResult.DONE, repository.drain())
        assertEquals(MessageStatus.FAILED, row("a").status)
        assertNull(outbox.get("a"))
    }

    @Test
    fun givesUpAfterEightFailedAttempts() = runBlocking {
        pending("a")
        repeat(7) {
            api.failures += IOException("offline")
            assertEquals(DrainResult.RETRY, repository.drain())
        }
        api.failures += IOException("offline")

        assertEquals(DrainResult.DONE, repository.drain())
        assertEquals(MessageStatus.FAILED, row("a").status)
        assertNull(outbox.get("a"))
    }

    @Test
    fun lostAckThenRetryWithKeyStoresOnce() = runBlocking {
        pending("a")
        api.lostAcks = 1

        assertEquals(DrainResult.RETRY, repository.drain())
        assertEquals(DrainResult.DONE, repository.drain())

        assertEquals(1, api.stored.size)
        assertEquals(1, messages.rows.size)
        assertEquals(MessageStatus.SENT, row("a").status)
    }

    @Test
    fun lostAckThenRetryWithoutKeyStoresTwice() = runBlocking {
        repository.sendWithoutKey = true
        pending("a")
        api.lostAcks = 1

        assertEquals(DrainResult.RETRY, repository.drain())
        assertEquals(DrainResult.DONE, repository.drain())

        // The server now holds two rows, so the sync after the ack adds the first as its own bubble.
        assertEquals(2, api.stored.size)
        assertEquals(listOf(1L, 2L), messages.rows.mapNotNull { it.serverSeq }.sorted())
    }

    // drain, the queue

    @Test
    fun twoMessagesQueuedOfflineReachTheServerInTapOrder() = runBlocking {
        api.offline = true
        repository.send("first")
        repository.send("second")
        repository.send("third")
        assertTrue(api.stored.isEmpty())
        assertEquals(listOf(DEFAULT_CONVERSATION_ID), scheduled.distinct())

        api.offline = false
        assertEquals(DrainResult.DONE, repository.drain())

        assertEquals(listOf("first", "second", "third"), api.stored.map { it.text })
        assertEquals(listOf("first", "second", "third"), messages.rows.sortedBy { it.serverSeq }.map { it.text })
        assertTrue(messages.rows.all { it.status == MessageStatus.SENT })
    }

    @Test
    fun aMessageTappedWhileAnotherIsOnTheWireWaitsForIt() = runBlocking {
        api.gate = CompletableDeferred()
        val held = api.gate!!
        repository.send("first")
        repository.send("second")
        // The first send is held on the wire; the second must not slip past it.
        assertEquals(1, api.sendCalls)

        held.complete(Unit)

        assertEquals(listOf("first", "second"), api.stored.map { it.text })
        assertEquals(2, api.sendCalls)
    }

    @Test
    fun retryableFailureOnTheHeadBlocksTheSecond() = runBlocking {
        pending("a")
        pending("b")
        api.failures += IOException("offline")

        assertEquals(DrainResult.RETRY, repository.drain())

        assertEquals(1, api.sendCalls)
        assertTrue(api.stored.isEmpty())
        assertEquals(MessageStatus.SENDING, row("b").status)
        assertEquals(0, outbox.get("b")?.attempts)

        assertEquals(DrainResult.DONE, repository.drain())
        assertEquals(listOf("a", "b"), api.stored.map { it.clientId })
    }

    @Test
    fun finalRejectionOnTheHeadLetsTheSecondGo() = runBlocking {
        pending("a")
        pending("b")
        api.failures += failureFor(400)

        assertEquals(DrainResult.DONE, repository.drain())

        assertEquals(MessageStatus.FAILED, row("a").status)
        assertEquals(MessageStatus.SENT, row("b").status)
        assertEquals(listOf("b"), api.stored.map { it.clientId })
    }

    @Test
    fun theEighthFailureOfTheHeadFreesTheQueue() = runBlocking {
        pending("a")
        pending("b")
        repeat(7) {
            api.failures += IOException("offline")
            assertEquals(DrainResult.RETRY, repository.drain())
        }
        api.failures += IOException("offline")

        assertEquals(DrainResult.DONE, repository.drain())

        assertEquals(MessageStatus.FAILED, row("a").status)
        assertNull(outbox.get("a"))
        assertEquals(MessageStatus.SENT, row("b").status)
    }

    @Test
    fun drainOnAnEmptyOutboxDoesNothing() = runBlocking {
        assertEquals(DrainResult.DONE, repository.drain())
        assertEquals(0, api.sendCalls)
    }

    // resumePending and tap on a FAILED bubble

    @Test
    fun resumePendingDrainsWhatTheProcessLeftBehind() = runBlocking {
        pending("a")
        pending("b")

        repository.resumePending()

        assertEquals(listOf("a", "b"), api.stored.map { it.clientId })
    }

    @Test
    fun retrySendsAFailedMessageAgain() = runBlocking {
        pending("a")
        api.failures += failureFor(400)
        repository.drain()

        repository.retry("a")

        assertEquals(MessageStatus.SENT, row("a").status)
        assertEquals(1, api.stored.size)
    }

    @Test
    fun retryStartsOverWithAFreshAttemptCount() = runBlocking {
        pending("a")
        repeat(8) {
            api.failures += IOException("offline")
            repository.drain()
        }
        assertEquals(MessageStatus.FAILED, row("a").status)
        scheduled.clear()
        api.failures += IOException("offline")

        repository.retry("a")

        assertEquals(MessageStatus.SENDING, row("a").status)
        assertEquals(1, outbox.get("a")?.attempts)
        assertEquals(listOf(DEFAULT_CONVERSATION_ID), scheduled)
    }

    @Test
    fun retryLeavesASentMessageAlone() = runBlocking {
        pending("a")
        repository.drain()
        val calls = api.sendCalls

        repository.retry("a")

        assertEquals(MessageStatus.SENT, row("a").status)
        assertEquals(calls, api.sendCalls)
        assertTrue(scheduled.isEmpty())
    }

    // which HTTP codes are worth another try

    @Test
    fun onlySomeStatusCodesRetry() {
        listOf(408, 425, 429, 500, 502, 503).forEach { assertTrue("$it", failureFor(it) is IOException) }
        listOf(400, 401, 403, 404, 422).forEach { assertTrue("$it", failureFor(it) is ServerRejectedException) }
    }
}
