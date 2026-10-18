package dev.logcatdaily.samples.fileupload

import android.util.Log
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RunResult { Done, Retry, Failed, Cancelled }

private const val TAG = "logcatdaily"
private const val MAX_CONFLICTS_IN_A_ROW = 3

// One run of one upload. The flags in UploadConfig decide what a run does after
// a failure; the loop is the same for every variant.
class UploadRunner(
    private val dao: UploadDao,
    private val api: UploadApi,
    private val config: UploadConfig,
    private val onProgress: suspend (UploadEntity, Long) -> Unit = { _, _ -> },
) {

    suspend fun run(id: String): RunResult {
        val queued = dao.get(id) ?: return RunResult.Done
        when (queued.state) {
            UploadState.CANCELLED -> return RunResult.Cancelled
            UploadState.DONE, UploadState.FAILED -> return RunResult.Done
            else -> Unit
        }
        dao.startRun(id)
        val row = dao.get(id) ?: return RunResult.Done
        log(row, "start variant=${config.variant} size=${row.totalBytes} sha256=${row.sha256.take(8)}")
        return try {
            if (config.chunked) runChunked(row) else runSingle(row)
        } catch (e: CancellationException) {
            // The user's cancel is already in Room and stays; a stop by the
            // system puts the row back to WAITING.
            withContext(NonCancellable) { dao.markWaitingIfUploading(id) }
            throw e
        }
    }

    // Nothing is remembered on purpose: the next run starts at byte 0.
    private suspend fun runSingle(row: UploadEntity): RunResult {
        dao.setConfirmed(row.id, 0)
        val reply = coroutineScope {
            val sent = MutableStateFlow(0L)
            // Room is written from here, not from the blocking write.
            val writer = launch {
                sent.collect {
                    dao.setConfirmed(row.id, it)
                    onProgress(row, it)
                }
            }
            try {
                send(row) { api.uploadWhole(File(row.sourcePath)) { bytes -> sent.value = bytes } }
            } finally {
                writer.cancel()
            }
        }
        if (reply is ApiReply.Ok) return done(row, "DONE")
        return settle(row, RetryClassifier.classify(config, Call.WHOLE, reply))
    }

    private suspend fun runChunked(row: UploadEntity): RunResult {
        var sessionUrl = row.sessionUrl
        if (sessionUrl == null) {
            val key = if (config.idempotentCreate) row.idempotencyKey else null
            val sha = if (config.finalCheck) row.sha256 else ""
            val reply = send(row) { api.createSession(row.totalBytes, sha, key) }
            if (reply is ApiReply.Ok && reply.location == null) return settle(row, Action.Fail("no Location in reply"))
            if (reply !is ApiReply.Ok) return settle(row, RetryClassifier.classify(config, Call.CREATE, reply))
            sessionUrl = reply.location ?: return settle(row, Action.Fail("no Location in reply"))
            // Saved before the first chunk, so a restart finds the session.
            dao.setSession(row.id, sessionUrl)
            val tag = "session ${sessionUrl.substringAfterLast('/')}"
            val keyTag = key?.take(4) ?: "none"
            if (reply.code == 200) log(row, "$tag reused, create was repeated key=$keyTag") else log(row, "$tag created key=$keyTag")
        }

        var serverOffset: Long? = null
        if (config.askServerOffset) {
            when (val asked = askOffset(row, sessionUrl, row.confirmedBytes)) {
                is Asked.Offset -> serverOffset = asked.value
                is Asked.Stop -> return asked.result
            }
        }
        val start = ResumeDecision.start(config, row.confirmedBytes, serverOffset, row.totalBytes)
        if (start !is Start.From) return settle(row, Action.Fail("bad offset"))
        var offset = start.offset
        // It may go down: a server that lost data wins.
        dao.setConfirmed(row.id, offset)

        var conflicts = 0
        while (offset < row.totalBytes) {
            val chunk = ChunkPlan.next(offset, row.totalBytes, config.chunkBytes) ?: break
            val last = chunk.offset + chunk.length == row.totalBytes
            val data = try {
                readChunk(row.sourcePath, chunk)
            } catch (e: IOException) {
                return settle(row, Action.Fail("file unreadable"))
            }
            val reply = send(row) { api.sendChunk(sessionUrl, chunk.offset, data, chunk.length) }
            val head = "PATCH offset=${chunk.offset}"
            when (reply) {
                is ApiReply.Ok -> {
                    conflicts = 0
                    // counter trusts its own count, server takes the reply's.
                    val now = if (config.askServerOffset)
                        reply.offset ?: (chunk.offset + chunk.length)
                    else
                        chunk.offset + chunk.length
                    offset = now
                    dao.setConfirmed(row.id, now)
                    onProgress(row, now)
                    log(row, "$head len=${chunk.length} -> ${reply.code} now=$now")
                }
                is ApiReply.Conflict -> {
                    log(row, "$head -> 409 conflict")
                    val action = RetryClassifier.classify(config, Call.CHUNK, reply)
                    if (action !is Action.AskServer) return settle(row, action)
                    if (++conflicts > MAX_CONFLICTS_IN_A_ROW) return settle(row, Action.Retry)
                    when (val asked = askOffset(row, sessionUrl, offset)) {
                        is Asked.Offset -> {
                            offset = asked.value
                            dao.setConfirmed(row.id, offset)
                        }
                        is Asked.Stop -> return asked.result
                    }
                }
                else -> return settle(row, RetryClassifier.classify(config, if (last) Call.LAST_CHUNK else Call.CHUNK, reply))
            }
        }
        dao.setConfirmed(row.id, row.totalBytes)
        return done(row, if (config.finalCheck) "DONE sha256 matches" else "DONE")
    }

    private sealed interface Asked {
        class Offset(val value: Long) : Asked
        class Stop(val result: RunResult) : Asked
    }

    private suspend fun askOffset(row: UploadEntity, sessionUrl: String, mine: Long): Asked {
        val reply = send(row) { api.queryOffset(sessionUrl) }
        if (reply !is ApiReply.Ok) return Asked.Stop(settle(row, RetryClassifier.classify(config, Call.HEAD, reply)))
        val offset = reply.offset ?: return Asked.Stop(settle(row, Action.Fail("no offset in reply")))
        log(row, "server offset=$offset, mine=$mine")
        return Asked.Offset(offset)
    }

    // A thrown IOException becomes null: reset, timeout, no route.
    private suspend fun send(row: UploadEntity, call: suspend () -> ApiReply): ApiReply? =
        try {
            call()
        } catch (e: IOException) {
            val confirmed = dao.get(row.id)?.confirmedBytes ?: row.confirmedBytes
            log(row, "network lost (${e.javaClass.simpleName}), confirmed=$confirmed, attempt=${row.attempts}")
            null
        }

    private suspend fun settle(row: UploadEntity, action: Action): RunResult = when (action) {
        Action.Retry -> {
            dao.setState(row.id, UploadState.WAITING)
            RunResult.Retry
        }
        Action.CountAttempt ->
            if (row.attempts >= config.maxAttempts) {
                settle(row, Action.Fail("server offset differs"))
            } else {
                dao.setState(row.id, UploadState.WAITING, "server offset differs")
                RunResult.Retry
            }
        // The runner handles AskServer itself; arriving here is a wiring bug.
        Action.AskServer -> error("AskServer is handled in the loop")
        is Action.Fail -> {
            dao.setState(row.id, UploadState.FAILED, action.error)
            log(row, "FAILED ${action.error}")
            RunResult.Failed
        }
    }

    private suspend fun done(row: UploadEntity, line: String): RunResult {
        dao.setConfirmed(row.id, row.totalBytes)
        dao.setState(row.id, UploadState.DONE)
        log(row, line)
        return RunResult.Done
    }

    private suspend fun readChunk(path: String, chunk: Chunk): ByteArray = withContext(Dispatchers.IO) {
        RandomAccessFile(path, "r").use { file ->
            file.seek(chunk.offset)
            ByteArray(chunk.length).also { file.readFully(it) }
        }
    }

    private fun log(row: UploadEntity, line: String) {
        Log.i(TAG, "Upload ${row.id} $line")
    }
}
