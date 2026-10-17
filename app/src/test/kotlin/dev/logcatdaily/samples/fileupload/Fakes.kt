package dev.logcatdaily.samples.fileupload

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// In-memory stand-in for the Room DAO. The SQL guards are repeated here, so a
// CANCELLED row stays CANCELLED in the tests the way it does in Room.
class FakeUploadDao : UploadDao {
    private val rows = MutableStateFlow<Map<String, UploadEntity>>(emptyMap())
    val confirmedHistory = mutableListOf<Long>()

    override suspend fun insert(upload: UploadEntity) {
        rows.value = rows.value + (upload.id to upload)
    }

    override suspend fun get(id: String): UploadEntity? = rows.value[id]

    override suspend fun count(): Int = rows.value.size

    override fun observeAll(): Flow<List<UploadEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAt } }

    override suspend fun setState(id: String, state: UploadState, lastError: String?) =
        change(id, { it.state != UploadState.CANCELLED }) { it.copy(state = state, lastError = lastError) }

    override suspend fun startRun(id: String) =
        change(id, { it.state != UploadState.CANCELLED }) { it.copy(state = UploadState.UPLOADING, attempts = it.attempts + 1, lastError = null) }

    override suspend fun setConfirmed(id: String, bytes: Long) {
        confirmedHistory += bytes
        change(id, { true }) { it.copy(confirmedBytes = bytes) }
    }

    override suspend fun setSession(id: String, url: String?) = change(id, { true }) { it.copy(sessionUrl = url) }

    override suspend fun markWaitingIfUploading(id: String) =
        change(id, { it.state == UploadState.UPLOADING }) { it.copy(state = UploadState.WAITING) }

    override suspend fun cancel(id: String) =
        change(id, { it.state != UploadState.DONE }) { it.copy(state = UploadState.CANCELLED, lastError = null) }

    override suspend fun reset(id: String) =
        change(id, { it.state == UploadState.FAILED }) {
            it.copy(state = UploadState.QUEUED, attempts = 0, confirmedBytes = 0, sessionUrl = null, lastError = null)
        }

    private fun change(id: String, guard: (UploadEntity) -> Boolean, edit: (UploadEntity) -> UploadEntity) {
        val row = rows.value[id] ?: return
        if (guard(row)) rows.value = rows.value + (id to edit(row))
    }
}

class FakeSession(val id: String, val length: Long, val sha256: String, val key: String?) {
    val received = ByteArrayOutputStream()
    val offset get() = received.size().toLong()
    var complete = false
}

// An in-memory server with the same rules as the mock: offsets, 409, an
// idempotent create, sha256 on the last chunk. Each knob fires once.
class FakeUploadApi : UploadApi("http://unused") {
    val sessions = mutableMapOf<String, FakeSession>()
    val createKeys = mutableListOf<String?>()
    var sessionsCreated = 0
    var conflicts = 0
    var sentBytes = 0L

    var failWholeAtBytes: Long? = null
    var failBeforeOffset: Long? = null
    var dropReplyAt: Long? = null
    var dropCreateReply = false
    var goneOnHead = false

    override suspend fun createSession(totalBytes: Long, sha256: String, idempotencyKey: String?): ApiReply {
        createKeys += idempotencyKey
        val known = idempotencyKey?.let { key -> sessions.values.firstOrNull { it.key == key } }
        val session = known ?: FakeSession("s-%04d".format(++sessionsCreated), totalBytes, sha256, idempotencyKey).also { sessions[it.id] = it }
        if (dropCreateReply) {
            dropCreateReply = false
            throw IOException("reply dropped")
        }
        return ApiReply.Ok(if (known != null) 200 else 201, null, "/files/${session.id}")
    }

    override suspend fun queryOffset(sessionUrl: String): ApiReply {
        val session = sessions[sessionUrl.substringAfterLast('/')]
        if (goneOnHead || session == null) return ApiReply.Gone(404)
        return ApiReply.Ok(200, session.offset, null)
    }

    override suspend fun sendChunk(sessionUrl: String, offset: Long, data: ByteArray, length: Int): ApiReply {
        val session = sessions[sessionUrl.substringAfterLast('/')] ?: return ApiReply.Gone(404)
        if (failBeforeOffset == offset) {
            failBeforeOffset = null
            throw IOException("connection reset")
        }
        if (offset != session.offset) {
            conflicts++
            return ApiReply.Conflict(session.offset)
        }
        session.received.write(data, 0, length)
        sentBytes += length
        if (session.offset == session.length) {
            val hash = MessageDigest.getInstance("SHA-256").digest(session.received.toByteArray()).joinToString("") { "%02x".format(it) }
            if (hash != session.sha256) return ApiReply.Rejected(422)
            session.complete = true
        }
        if (dropReplyAt == offset) {
            dropReplyAt = null
            throw IOException("reply dropped")
        }
        return ApiReply.Ok(204, session.offset, null)
    }

    val deleted = mutableListOf<String>()

    override suspend fun deleteSession(sessionUrl: String): ApiReply {
        deleted += sessionUrl
        sessions.remove(sessionUrl.substringAfterLast('/'))
        return ApiReply.Ok(204, null, null)
    }

    override suspend fun uploadWhole(file: File, onSent: (Long) -> Unit): ApiReply {
        val cut = failWholeAtBytes
        if (cut != null) {
            failWholeAtBytes = null
            sentBytes += cut
            throw IOException("connection reset")
        }
        sentBytes += file.length()
        return ApiReply.Ok(201, null, null)
    }
}
