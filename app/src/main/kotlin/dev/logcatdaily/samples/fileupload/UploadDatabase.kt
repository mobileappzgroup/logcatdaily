package dev.logcatdaily.samples.fileupload

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(entities = [UploadEntity::class], version = 1, exportSchema = false)
abstract class UploadDatabase : RoomDatabase() {
    abstract fun uploadDao(): UploadDao
}

// Lets the repository run its multi-step writes without knowing about Room.
interface Transactor {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactor(private val db: UploadDatabase) : Transactor {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}
