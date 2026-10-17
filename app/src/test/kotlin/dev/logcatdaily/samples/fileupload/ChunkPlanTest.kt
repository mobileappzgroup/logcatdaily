package dev.logcatdaily.samples.fileupload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkPlanTest {
    private val mib = 1_048_576
    private val total = 24L * mib

    @Test
    fun `24 MiB at 1 MiB is 24 chunks`() {
        var offset = 0L
        var chunks = 0
        while (true) {
            val chunk = ChunkPlan.next(offset, total, mib) ?: break
            assertEquals(mib, chunk.length)
            offset += chunk.length
            chunks++
        }
        assertEquals(24, chunks)
        assertEquals(total, offset)
    }

    @Test
    fun `the last chunk is shorter when the file is not a multiple`() {
        val chunk = ChunkPlan.next(2L * mib, 2L * mib + 500, mib)
        assertNotNull(chunk)
        assertEquals(500, chunk!!.length)
    }

    @Test
    fun `an offset in the middle of a chunk ends at the next boundary`() {
        val chunk = ChunkPlan.next(mib + 300_000L, total, mib)!!
        assertEquals(mib + 300_000L, chunk.offset)
        assertEquals(mib - 300_000, chunk.length)
    }

    @Test
    fun `an offset in the middle of the last chunk ends at the file end`() {
        val chunk = ChunkPlan.next(mib + 100L, mib + 500L, mib)!!
        assertEquals(400, chunk.length)
    }

    @Test
    fun `an offset equal to the total returns null`() {
        assertNull(ChunkPlan.next(total, total, mib))
        assertNull(ChunkPlan.next(total + 1, total, mib))
        assertNull(ChunkPlan.next(-1, total, mib))
    }

    @Test
    fun `length never passes chunk size or what is left`() {
        for (offset in listOf(0L, 1L, 999_999L, mib.toLong(), total - 1, total - mib)) {
            val chunk = ChunkPlan.next(offset, total, mib)!!
            assertTrue(chunk.length <= mib)
            assertTrue(chunk.length <= total - offset)
        }
    }
}
