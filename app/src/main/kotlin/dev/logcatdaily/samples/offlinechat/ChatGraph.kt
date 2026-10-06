package dev.logcatdaily.samples.offlinechat

import android.content.Context
import androidx.room.Room
import androidx.work.ExistingWorkPolicy
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
        // Sample data only: a schema change wipes the database.
        val db = Room.databaseBuilder(context, ChatDatabase::class.java, "chat.db")
            .fallbackToDestructiveMigration(true)
            .build()
        val workManager = WorkManager.getInstance(context)
        return MessageRepository(
            messages = db.messageDao(),
            outbox = db.outboxDao(),
            api = ChatApi(),
            transactor = RoomTransactor(db),
            scheduleRetry = { clientId ->
                workManager.enqueueUniqueWork(
                    SendWorker.uniqueName(clientId),
                    ExistingWorkPolicy.KEEP,
                    SendWorker.request(clientId),
                )
            },
        )
    }
}
