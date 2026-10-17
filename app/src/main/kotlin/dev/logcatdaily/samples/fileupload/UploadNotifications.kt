package dev.logcatdaily.samples.fileupload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

private const val CHANNEL_ID = "uploads"

object UploadNotifications {

    fun notificationId(uploadId: String): Int = 1000 + (uploadId.removePrefix("u-").toIntOrNull() ?: 0)

    fun foregroundInfo(context: Context, upload: UploadEntity, confirmed: Long): ForegroundInfo {
        val id = notificationId(upload.id)
        val notification = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Uploading ${upload.fileName}")
            .setContentText(progressText(confirmed, upload.totalBytes))
            .setProgress(upload.totalBytes.toInt(), confirmed.toInt(), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Cancel", cancelIntent(context, upload.id))
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    fun finished(context: Context, upload: UploadEntity) {
        val notification = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("Upload finished")
            .setContentText(upload.fileName)
            .setAutoCancel(true)
            .build()
        manager(context).notify(notificationId(upload.id), notification)
    }

    fun clear(context: Context, uploadId: String) = manager(context).cancel(notificationId(uploadId))

    private fun cancelIntent(context: Context, uploadId: String): PendingIntent {
        val intent = Intent(context, CancelUploadReceiver::class.java).putExtra(CancelUploadReceiver.EXTRA_ID, uploadId)
        return PendingIntent.getBroadcast(
            context,
            notificationId(uploadId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun channel(context: Context): String {
        manager(context).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Uploads", NotificationManager.IMPORTANCE_LOW),
        )
        return CHANNEL_ID
    }

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
}
