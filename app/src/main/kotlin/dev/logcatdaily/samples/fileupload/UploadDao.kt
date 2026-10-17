package dev.logcatdaily.samples.fileupload

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {

    @Insert
    suspend fun insert(upload: UploadEntity)

    @Query("SELECT * FROM uploads WHERE id = :id")
    suspend fun get(id: String): UploadEntity?

    @Query("SELECT COUNT(*) FROM uploads")
    suspend fun count(): Int

    // The screen reads only this.
    @Query("SELECT * FROM uploads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UploadEntity>>

    // CANCELLED is final: no late write from a run that was still in flight
    // moves a row out of it. Only the user's Add starts a new row.
    @Query("UPDATE uploads SET state = :state, lastError = :lastError WHERE id = :id AND state != 'CANCELLED'")
    suspend fun setState(id: String, state: UploadState, lastError: String? = null)

    @Query("UPDATE uploads SET state = 'UPLOADING', attempts = attempts + 1, lastError = NULL WHERE id = :id AND state != 'CANCELLED'")
    suspend fun startRun(id: String)

    @Query("UPDATE uploads SET confirmedBytes = :bytes WHERE id = :id")
    suspend fun setConfirmed(id: String, bytes: Long)

    @Query("UPDATE uploads SET sessionUrl = :url WHERE id = :id")
    suspend fun setSession(id: String, url: String?)

    // A stopped run puts UPLOADING back to WAITING. A row that is already
    // CANCELLED, DONE or FAILED stays as it is.
    @Query("UPDATE uploads SET state = 'WAITING' WHERE id = :id AND state = 'UPLOADING'")
    suspend fun markWaitingIfUploading(id: String)

    @Query("UPDATE uploads SET state = 'CANCELLED', lastError = NULL WHERE id = :id AND state != 'DONE'")
    suspend fun cancel(id: String)

    // Retry on a FAILED row: start over with a new session.
    @Query("UPDATE uploads SET state = 'QUEUED', attempts = 0, confirmedBytes = 0, sessionUrl = NULL, lastError = NULL WHERE id = :id AND state = 'FAILED'")
    suspend fun reset(id: String)
}
