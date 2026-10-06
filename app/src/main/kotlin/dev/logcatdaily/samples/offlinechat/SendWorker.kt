package dev.logcatdaily.samples.offlinechat

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// The retry path. The foreground drain already tried, this runs the same drain
// once the device has a connection. One unique work per conversation.
class SendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        when (ChatGraph.repository(applicationContext).drain()) {
            DrainResult.DONE -> Result.success()
            DrainResult.RETRY -> Result.retry()
        }

    companion object {
        fun uniqueName(conversationId: String) = "outbox-$conversationId"

        fun request(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SendWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
    }
}
