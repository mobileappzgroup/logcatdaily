package dev.logcatdaily.samples.offlinechat

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

// The retry path. The repository already tried once in the foreground, this
// runs when that failed and the device has a connection.
class SendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val clientId = inputData.getString(KEY_CLIENT_ID) ?: return Result.failure()
        val repository = ChatGraph.repository(applicationContext)
        return when (repository.attemptSend(clientId)) {
            SendOutcome.DONE -> Result.success()
            SendOutcome.RETRY -> Result.retry()
            SendOutcome.GAVE_UP -> Result.failure()
        }
    }

    companion object {
        private const val KEY_CLIENT_ID = "clientId"

        fun uniqueName(clientId: String) = "send-$clientId"

        fun request(clientId: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(workDataOf(KEY_CLIENT_ID to clientId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
    }
}
