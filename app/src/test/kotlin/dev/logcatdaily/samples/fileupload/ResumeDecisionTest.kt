package dev.logcatdaily.samples.fileupload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeDecisionTest {
    private val total = 25_165_824L

    private fun offsetOf(start: Start) = (start as Start.From).offset

    @Test
    fun `counter starts at the row and ignores the server`() {
        val start = ResumeDecision.start(UploadConfig.COUNTER, 9_437_184, 10_485_760, total)
        assertEquals(9_437_184L, offsetOf(start))
    }

    @Test
    fun `server starts at the server offset`() {
        val start = ResumeDecision.start(UploadConfig.SERVER, 9_437_184, 10_485_760, total)
        assertEquals(10_485_760L, offsetOf(start))
    }

    @Test
    fun `server wins even when it is lower than the row`() {
        val start = ResumeDecision.start(UploadConfig.SERVER, 9_437_184, 4_194_304, total)
        assertEquals(4_194_304L, offsetOf(start))
    }

    @Test
    fun `an offset above the file size fails`() {
        assertTrue(ResumeDecision.start(UploadConfig.SERVER, 0, total + 1, total) is Start.Fail)
        assertTrue(ResumeDecision.start(UploadConfig.COUNTER, total + 1, null, total) is Start.Fail)
    }
}
