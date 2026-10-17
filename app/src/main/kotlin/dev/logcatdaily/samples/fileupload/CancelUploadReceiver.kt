package dev.logcatdaily.samples.fileupload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// The Cancel button in the notification. Not exported: only our own
// PendingIntent reaches it.
class CancelUploadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val pending = goAsync()
        val repository = UploadGraph.repository(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.cancel(id)
            } finally {
                UploadNotifications.clear(context.applicationContext, id)
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "uploadId"
    }
}
