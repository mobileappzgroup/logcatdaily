package dev.logcatdaily.samples.fileupload

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val PREFS = "fileupload"
private const val KEY_VARIANT = "variant"
private const val KEY_CHUNK_KB = "chunkKb"

// Hand-wired, no DI library. The worker can run in a fresh process, so the
// variant chosen at launch is kept in preferences and read again here.
object UploadGraph {
    @Volatile
    private var repository: UploadRepository? = null

    // chunkKb is for quick tests; 0 keeps the 1 MiB default the video uses.
    fun configure(context: Context, newVariant: String?, chunkKb: Int = 0) {
        val name = UploadConfig.forVariant(newVariant).variant
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // A rotation calls this again with the same values: keep the graph.
        if (prefs.getString(KEY_VARIANT, null) == name && prefs.getInt(KEY_CHUNK_KB, 0) == chunkKb) return
        prefs.edit().putString(KEY_VARIANT, name).putInt(KEY_CHUNK_KB, chunkKb).apply()
        synchronized(this) {
            repository = null
        }
    }

    fun repository(context: Context): UploadRepository =
        repository ?: synchronized(this) {
            repository ?: build(context.applicationContext).also { repository = it }
        }

    fun config(context: Context): UploadConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val config = UploadConfig.forVariant(prefs.getString(KEY_VARIANT, null))
        val chunkKb = prefs.getInt(KEY_CHUNK_KB, 0)
        return if (chunkKb > 0) config.copy(chunkBytes = chunkKb * 1024) else config
    }

    private fun build(context: Context): UploadRepository {
        val config = config(context)
        // Sample data only: a schema change wipes the database.
        val db = Room.databaseBuilder(context, UploadDatabase::class.java, "uploads.db")
            .fallbackToDestructiveMigration(true)
            .build()
        return UploadRepository(
            dao = db.uploadDao(),
            api = UploadApi(),
            config = config,
            transactor = RoomTransactor(db),
            workManager = WorkManager.getInstance(context),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}
