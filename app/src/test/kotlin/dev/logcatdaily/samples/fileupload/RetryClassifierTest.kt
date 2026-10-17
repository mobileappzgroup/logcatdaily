package dev.logcatdaily.samples.fileupload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryClassifierTest {
    private val server = UploadConfig.SERVER
    private val counter = UploadConfig.COUNTER

    @Test
    fun `network errors and busy servers retry`() {
        assertTrue(RetryClassifier.classify(server, Call.CHUNK, null) is Action.Retry)
        assertTrue(RetryClassifier.classify(server, Call.CHUNK, ApiReply.ServerError(408)) is Action.Retry)
        assertTrue(RetryClassifier.classify(server, Call.CHUNK, ApiReply.ServerError(425)) is Action.Retry)
        assertTrue(RetryClassifier.classify(server, Call.CHUNK, ApiReply.ServerError(429)) is Action.Retry)
        assertTrue(RetryClassifier.classify(server, Call.CREATE, ApiReply.ServerError(503)) is Action.Retry)
    }

    @Test
    fun `409 asks the server in server and counts an attempt in counter`() {
        assertTrue(RetryClassifier.classify(server, Call.CHUNK, ApiReply.Conflict(10)) is Action.AskServer)
        assertTrue(RetryClassifier.classify(counter, Call.CHUNK, ApiReply.Conflict(10)) is Action.CountAttempt)
    }

    @Test
    fun `404 and 410 on HEAD end as session gone`() {
        for (code in listOf(404, 410)) {
            val action = RetryClassifier.classify(server, Call.HEAD, ApiReply.Gone(code)) as Action.Fail
            assertEquals("session gone", action.error)
        }
    }

    @Test
    fun `422 on the last chunk is a checksum failure`() {
        val action = RetryClassifier.classify(server, Call.LAST_CHUNK, ApiReply.Rejected(422)) as Action.Fail
        assertEquals("checksum differs", action.error)
    }

    @Test
    fun `other 4xx are final`() {
        for (code in listOf(400, 401, 403, 413, 422)) {
            assertTrue(RetryClassifier.classify(server, Call.CHUNK, ApiReply.Rejected(code)) is Action.Fail)
        }
        val action = RetryClassifier.classify(server, Call.CREATE, ApiReply.Rejected(401)) as Action.Fail
        assertEquals("HTTP 401", action.error)
    }
}
