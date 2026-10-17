package dev.logcatdaily.samples.fileupload

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UploadState { QUEUED, UPLOADING, WAITING, DONE, FAILED, CANCELLED }

// One row per upload: the queue. The WorkManager request carries only the id,
// this row is the truth.
@Entity(tableName = "uploads")
data class UploadEntity(
    @PrimaryKey val id: String,
    // Created with the row, before any request leaves the phone.
    val idempotencyKey: String,
    val fileName: String,
    val sourcePath: String,
    val totalBytes: Long,
    val sha256: String,
    val sessionUrl: String? = null,
    // What the app believes the server has.
    val confirmedBytes: Long = 0,
    val state: UploadState = UploadState.QUEUED,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
)
