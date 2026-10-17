package dev.logcatdaily.samples.fileupload

import java.util.Locale

private const val MIB = 1024.0 * 1024.0

// "9.0 of 24.0 MB", on the row and in the notification.
fun progressText(done: Long, total: Long): String =
    String.format(Locale.US, "%.1f of %.1f MB", done / MIB, total / MIB)

fun stateText(upload: UploadEntity): String = when (upload.state) {
    UploadState.QUEUED -> "Waiting to start"
    UploadState.UPLOADING -> "Uploading"
    UploadState.WAITING ->
        if (upload.attempts > 1 && upload.lastError != null) "Waiting to retry, attempt ${upload.attempts}" else "Waiting for network"
    UploadState.DONE -> "Done. sha256 matches."
    UploadState.FAILED -> "Failed: ${upload.lastError ?: "unknown"}"
    UploadState.CANCELLED -> "Cancelled"
}
