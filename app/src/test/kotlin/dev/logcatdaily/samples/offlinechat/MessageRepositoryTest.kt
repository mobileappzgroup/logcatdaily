package dev.logcatdaily.samples.offlinechat

import java.io.IOException
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
    private val repository = MessageRepository(messages, outbox, api, DirectTransactor, { scheduled += it })

    // A message the way send() leaves it: a SENDING row and an outbox row.
    private fun pending(clientId: String, text: String = "hi") = runBlocking {
        messages.insert(
            Message(
                clientId = clientId,
                conversationId = DEFAULT_CONVERSATION_ID,
                senderId = PHONE_SENDER_ID,
                text = text,
                status = MessageStatus.SENDING,
                createdAt = 10,
            ),
        )
        outbox.insert(OutboxEntry(clientId))
    }

    private fun row(clientId: String) = messages.rows.single { it.clientId == clientId }

    // merge, seen through sync()

    @Test
    fun skipsARowThatIsAlreadyStored() = runBlocking {
        pending("a")
        repository.attemptSend("a")
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

    // attemptSend

    @Test
    fun successMarksTheMessageSent() = runBlocking {
        pending("a")

        assertEquals(SendOutcome.DONE, repository.attemptSend("a"))

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

        assertEquals(SendOutcome.RETRY, repository.attemptSend("a"))

        assertEquals(MessageStatus.SENDING, row("a").status)
        assertEquals(1, outbox.get("a")?.attempts)
    }

    @Test
    fun rateLimitedRetries() = runBlocking {
        pending("a")
        api.failures += failureFor(429)

        assertEquals(SendOutcome.RETRY, repository.attemptSend("a"))
        assertEquals(MessageStatus.SENDING, row("a").status)
    }

    @Test
    fun badRequestIsFinal() = runBlocking {
        pending("a")
        api.failures += failureFor(400)

        assertEquals(SendOutcome.GAVE_UP, repository.attemptSend("a"))
        assertEquals(MessageStatus.FAILED, row("a").status)
    }

    @Test
    fun givesUpAfterEightFailedAttempts() = runBlocking {
        pending("a")
        repeat(7) {
            api.failures += IOException("offline")
            assertEquals(SendOutcome.RETRY, repository.attemptSend("a"))
        }
        api.failures += IOException("offline")

        assertEquals(SendOutcome.GAVE_UP, repository.attemptSend("a"))
        assertEquals(MessageStatus.FAILED, row("a").status)
    }

    @Test
    fun lostAckThenRetryWithKeyStoresOnce() = runBlocking {
        pending("a")
        api.lostAcks = 1

        assertEquals(SendOutcome.RETRY, repository.attemptSend("a"))
        assertEquals(SendOutcome.DONE, repository.attemptSend("a"))

        assertEquals(1, api.stored.size)
        assertEquals(1, messages.rows.size)
        assertEquals(MessageStatus.SENT, row("a").status)
    }

    @Test
    fun lostAckThenRetryWithoutKeyStoresTwice() = runBlocking {
        repository.sendWithoutKey = true
        pending("a")
        api.lostAcks = 1

        assertEquals(SendOutcome.RETRY, repository.attemptSend("a"))
        assertEquals(SendOutcome.DONE, repository.attemptSend("a"))

        // The server now holds two rows, so the sync after the ack adds the first as its own bubble.
        assertEquals(2, api.stored.size)
        assertEquals(listOf(1L, 2L), messages.rows.mapNotNull { it.serverSeq }.sorted())
    }

    // tap on a FAILED bubble

    @Test
    fun retryResetsAFailedMessageAndQueuesTheWorker() = runBlocking {
        pending("a")
        api.failures += failureFor(400)
        repository.attemptSend("a")

        repository.retry("a")

        assertEquals(MessageStatus.SENDING, row("a").status)
        assertEquals(0, outbox.get("a")?.attempts)
        assertEquals(listOf("a"), scheduled)
    }

    @Test
    fun retryLeavesASentMessageAlone() = runBlocking {
        pending("a")
        repository.attemptSend("a")

        repository.retry("a")

        assertEquals(MessageStatus.SENT, row("a").status)
        assertTrue(scheduled.isEmpty())
    }

    // which HTTP codes are worth another try

    @Test
    fun onlySomeStatusCodesRetry() {
        listOf(408, 425, 429, 500, 502, 503).forEach { assertTrue("$it", failureFor(it) is IOException) }
        listOf(400, 401, 403, 404, 422).forEach { assertTrue("$it", failureFor(it) is ServerRejectedException) }
    }
}
