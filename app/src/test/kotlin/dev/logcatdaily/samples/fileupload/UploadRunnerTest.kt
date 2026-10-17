package dev.logcatdaily.samples.fileupload

import java.io.File
import java.util.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A small file and 1000-byte chunks keep these tests fast; the rules are the same.
class UploadRunnerTest {
    private val size = 10_500L
    private val file = File.createTempFile("upload", ".bin").also { it.writeBytes(ByteArray(size.toInt()).also { b -> Random(7).nextBytes(b) }) }
    private val dao = FakeUploadDao()
    private val api = FakeUploadApi()

    @After
    fun cleanUp() {
        file.delete()
    }

    private fun config(base: UploadConfig) = base.copy(chunkBytes = 1000)

    private fun row(sessionUrl: String? = null, confirmed: Long = 0) = UploadEntity(
        id = "u-0001",
        idempotencyKey = "7c1e-key",
        fileName = "nishant-trip.mp4",
        sourcePath = file.absolutePath,
        totalBytes = size,
        sha256 = TestFile.sha256(file),
        sessionUrl = sessionUrl,
        confirmedBytes = confirmed,
        createdAt = 1,
    )

    private fun runner(base: UploadConfig) = UploadRunner(dao, api, config(base))

    // WorkManager's job: run again after Retry, up to a limit.
    private suspend fun runUntilSettled(base: UploadConfig, limit: Int = 10): RunResult {
        var result = RunResult.Retry
        var runs = 0
        while (result == RunResult.Retry && runs++ < limit) result = runner(base).run("u-0001")
        return result
    }

    @Test
    fun `single restarts at byte 0 and sends about 1_6 times the file`() = runBlocking {
        dao.insert(row())
        api.failWholeAtBytes = 6_500
        assertEquals(RunResult.Retry, runner(UploadConfig.SINGLE).run("u-0001"))
        assertEquals(UploadState.WAITING, dao.get("u-0001")!!.state)
        assertEquals(RunResult.Done, runner(UploadConfig.SINGLE).run("u-0001"))
        assertEquals(6_500 + size, api.sentBytes)
        assertTrue(api.sentBytes > size * 1.5)
        assertEquals(UploadState.DONE, dao.get("u-0001")!!.state)
    }

    @Test
    fun `counter resumes after a failure at a chunk boundary`() = runBlocking {
        dao.insert(row())
        api.failBeforeOffset = 4_000
        assertEquals(RunResult.Retry, runner(UploadConfig.COUNTER).run("u-0001"))
        assertEquals(4_000L, dao.get("u-0001")!!.confirmedBytes)
        assertEquals(RunResult.Done, runner(UploadConfig.COUNTER).run("u-0001"))
        assertEquals(size, api.sentBytes)
        assertTrue(api.sessions.values.single().complete)
    }

    @Test
    fun `counter gets stuck on a lost reply and ends FAILED`() = runBlocking {
        dao.insert(row())
        api.dropReplyAt = 3_000
        assertEquals(RunResult.Failed, runUntilSettled(UploadConfig.COUNTER))
        val failed = dao.get("u-0001")!!
        assertEquals(UploadState.FAILED, failed.state)
        assertEquals("server offset differs", failed.lastError)
        assertEquals(6, failed.attempts)
        // The server has the chunk, the app's counter never moved past it.
        assertEquals(3_000L, failed.confirmedBytes)
        assertEquals(4_000L, api.sessions.values.single().offset)
        assertTrue(api.conflicts >= 5)
    }

    @Test
    fun `server finishes after the same lost reply`() = runBlocking {
        dao.insert(row())
        api.dropReplyAt = 3_000
        assertEquals(RunResult.Done, runUntilSettled(UploadConfig.SERVER))
        assertTrue(api.sentBytes <= size + 1_000)
        val session = api.sessions.values.single()
        assertTrue(session.complete)
        assertEquals(size, session.offset)
    }

    @Test
    fun `a repeated create gives one session with the key and two without`() = runBlocking {
        dao.insert(row())
        api.dropCreateReply = true
        assertEquals(RunResult.Done, runUntilSettled(UploadConfig.SERVER))
        assertEquals(1, api.sessionsCreated)

        val plainDao = FakeUploadDao()
        val plainApi = FakeUploadApi()
        plainDao.insert(row())
        plainApi.dropCreateReply = true
        var result = RunResult.Retry
        var runs = 0
        while (result == RunResult.Retry && runs++ < 10) result = UploadRunner(plainDao, plainApi, config(UploadConfig.COUNTER)).run("u-0001")
        assertEquals(RunResult.Done, result)
        // The orphan: the first session nobody finishes.
        assertEquals(2, plainApi.sessionsCreated)
    }

    @Test
    fun `404 on HEAD ends FAILED with session gone`() = runBlocking {
        dao.insert(row(sessionUrl = "/files/s-0001"))
        api.goneOnHead = true
        assertEquals(RunResult.Failed, runner(UploadConfig.SERVER).run("u-0001"))
        val failed = dao.get("u-0001")!!
        assertEquals(UploadState.FAILED, failed.state)
        assertEquals("session gone", failed.lastError)
    }

    @Test
    fun `a cancelled row is not run`() = runBlocking {
        dao.insert(row())
        dao.cancel("u-0001")
        assertEquals(RunResult.Cancelled, runner(UploadConfig.SERVER).run("u-0001"))
        assertEquals(0, api.createKeys.size)
        assertEquals(UploadState.CANCELLED, dao.get("u-0001")!!.state)
    }

    @Test
    fun `a late stop does not turn CANCELLED into WAITING`() = runBlocking {
        val inCall = CompletableDeferred<Unit>()
        val hanging = object : UploadApi("http://unused") {
            override suspend fun createSession(totalBytes: Long, sha256: String, idempotencyKey: String?): ApiReply {
                inCall.complete(Unit)
                CompletableDeferred<Unit>().await()
                error("unreachable")
            }
        }
        dao.insert(row())
        val job = launch(Dispatchers.Default) { UploadRunner(dao, hanging, config(UploadConfig.SERVER)).run("u-0001") }
        inCall.await()
        // The repository writes CANCELLED before WorkManager is told.
        dao.cancel("u-0001")
        job.cancelAndJoin()
        assertEquals(UploadState.CANCELLED, dao.get("u-0001")!!.state)
    }

    @Test
    fun `a stop by the system puts UPLOADING back to WAITING`() = runBlocking {
        val inCall = CompletableDeferred<Unit>()
        val hanging = object : UploadApi("http://unused") {
            override suspend fun createSession(totalBytes: Long, sha256: String, idempotencyKey: String?): ApiReply {
                inCall.complete(Unit)
                CompletableDeferred<Unit>().await()
                error("unreachable")
            }
        }
        dao.insert(row())
        val job = launch(Dispatchers.Default) { UploadRunner(dao, hanging, config(UploadConfig.SERVER)).run("u-0001") }
        inCall.await()
        job.cancelAndJoin()
        assertEquals(UploadState.WAITING, dao.get("u-0001")!!.state)
    }

    @Test
    fun `confirmedBytes goes down only when the server says so`() = runBlocking {
        // The server has 3000 bytes, the row believes 5000.
        val bytes = file.readBytes()
        val create = api.createSession(size, TestFile.sha256(file), "7c1e-key")
        val url = (create as ApiReply.Ok).location!!
        api.sendChunk(url, 0, bytes.copyOfRange(0, 3_000), 3_000)
        dao.insert(row(sessionUrl = url, confirmed = 5_000))
        dao.confirmedHistory.clear()
        assertEquals(RunResult.Done, runner(UploadConfig.SERVER).run("u-0001"))
        assertEquals(3_000L, dao.confirmedHistory.first())
        // After that it only climbs.
        assertEquals(dao.confirmedHistory.sorted(), dao.confirmedHistory)
    }

    @Test
    fun `counter trusts the row even when the server is behind`() = runBlocking {
        val bytes = file.readBytes()
        val create = api.createSession(size, TestFile.sha256(file), null)
        val url = (create as ApiReply.Ok).location!!
        api.sendChunk(url, 0, bytes.copyOfRange(0, 3_000), 3_000)
        dao.insert(row(sessionUrl = url, confirmed = 5_000))
        assertEquals(RunResult.Retry, runner(UploadConfig.COUNTER).run("u-0001"))
        assertEquals(1, api.conflicts)
        assertEquals("server offset differs", dao.get("u-0001")!!.lastError)
    }
}
