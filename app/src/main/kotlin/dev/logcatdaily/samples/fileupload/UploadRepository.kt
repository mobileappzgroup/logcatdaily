package dev.logcatdaily.samples.fileupload

import android.util.Log
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "logcatdaily"

// Owns the rules for state changes. The worker and the screen go through here.
class UploadRepository(
    private val dao: UploadDao,
    private val api: UploadApi,
    private val config: UploadConfig,
    private val transactor: Transactor,
    private val workManager: WorkManager,
    // The best-effort DELETE after a cancel must outlive the caller.
    private val scope: CoroutineScope,
) {

    fun observe(): Flow<List<UploadEntity>> = dao.observeAll()

    suspend fun get(id: String): UploadEntity? = dao.get(id)

    // The row and its idempotency key are saved in one transaction, before any
    // request leaves the phone. Then the work is told to run.
    suspend fun enqueue(source: File): String {
        val sha = withContext(Dispatchers.IO) { TestFile.sha256(source) }
        val id = transactor.run {
            val id = "u-%04d".format(dao.count() + 1)
            dao.insert(
                UploadEntity(
                    id = id,
                    idempotencyKey = UUID.randomUUID().toString(),
                    fileName = source.name,
                    sourcePath = source.absolutePath,
                    totalBytes = source.length(),
                    sha256 = sha,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            id
        }
        schedule(id)
        return id
    }

    // Room first, so a stop that follows can tell the user's cancel from the
    // system's. Then WorkManager. The DELETE comes last and may fail: the
    // server forgets idle sessions on its own.
    suspend fun cancel(id: String) {
        val row = dao.get(id)
        dao.cancel(id)
        workManager.cancelUniqueWork(workName(id))
        val sessionUrl = row?.sessionUrl
        if (sessionUrl == null) {
            Log.i(TAG, "Upload $id cancelled by user, no session")
            return
        }
        scope.launch {
            val deleted = try {
                api.deleteSession(sessionUrl) is ApiReply.Ok
            } catch (e: java.io.IOException) {
                false
            }
            if (deleted) Log.i(TAG, "Upload $id cancelled by user, session deleted")
            else Log.i(TAG, "Upload $id cancelled by user, session delete failed, server expiry will clean up")
        }
    }

    suspend fun retry(id: String) {
        dao.reset(id)
        schedule(id)
    }

    suspend fun run(id: String, onProgress: suspend (UploadEntity, Long) -> Unit): RunResult =
        UploadRunner(dao, api, config, onProgress).run(id)

    private fun schedule(id: String) {
        workManager.enqueueUniqueWork(workName(id), ExistingWorkPolicy.KEEP, UploadWorker.request(id))
    }

    companion object {
        fun workName(id: String) = "upload-$id"
    }
}
