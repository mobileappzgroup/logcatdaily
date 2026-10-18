package dev.logcatdaily.samples.fileupload

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

private const val TAG = "logcatdaily"
private const val KEY_ID = "uploadId"

// Thin on purpose: read the id, call run, map the result. The row is the truth.
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val repository = UploadGraph.repository(applicationContext)
        val upload = repository.get(id) ?: return Result.failure()

        var foreground = try {
            setForeground(UploadNotifications.foregroundInfo(applicationContext, upload, upload.confirmedBytes))
            Log.i(TAG, "Upload $id foreground started, type=dataSync")
            true
        } catch (e: IllegalStateException) {
            // Started from the background: the system refuses a foreground service.
            Log.i(TAG, "Upload $id foreground not allowed, running as a plain worker")
            false
        }

        val result = try {
            repository.run(id) { row, confirmed ->
                if (foreground) setForeground(UploadNotifications.foregroundInfo(applicationContext, row, confirmed))
            }
        } catch (e: CancellationException) {
            // WorkManager stopped the work: the network constraint went away, the
            // user cancelled, or the system ran out of patience. The row decides what's next.
            Log.i(TAG, "Upload $id stopped by WorkManager, reason=${stopReasonName(stopReason)}")
            throw e
        }
        return when (result) {
            RunResult.Done -> {
                repository.get(id)?.takeIf { it.state == UploadState.DONE }?.let { UploadNotifications.finished(applicationContext, it) }
                Result.success()
            }
            RunResult.Retry -> Result.retry()
            RunResult.Failed -> Result.failure()
            RunResult.Cancelled -> Result.success()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val id = inputData.getString(KEY_ID).orEmpty()
        val upload = UploadGraph.repository(applicationContext).get(id)
            ?: throw IllegalStateException("no upload row for $id")
        return UploadNotifications.foregroundInfo(applicationContext, upload, upload.confirmedBytes)
    }

    companion object {
        private fun stopReasonName(reason: Int): String = when (reason) {
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "no network"
            WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "cancelled by the app"
            WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT -> "foreground service timeout"
            WorkInfo.STOP_REASON_TIMEOUT -> "timeout"
            else -> "code $reason"
        }

        fun request(id: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(Data.Builder().putString(KEY_ID, id).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
    }
}
