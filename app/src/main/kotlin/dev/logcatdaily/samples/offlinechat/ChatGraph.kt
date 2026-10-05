package dev.logcatdaily.samples.offlinechat

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager

// The ViewModel and the worker must share one database and one api object.
object ChatGraph {
    @Volatile
    private var repository: MessageRepository? = null

    fun repository(context: Context): MessageRepository =
        repository ?: synchronized(this) {
            repository ?: build(context.applicationContext).also { repository = it }
        }

    private fun build(context: Context): MessageRepository {
        val db = Room.databaseBuilder(context, ChatDatabase::class.java, "chat.db").build()
        return MessageRepository(db, ChatApi(), WorkManager.getInstance(context))
    }
}
