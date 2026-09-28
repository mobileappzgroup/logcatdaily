package dev.logcatdaily.samples.anrroom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction

@Dao
abstract class NoteDao {

    @Insert
    abstract fun insertRaw(note: Note): Long

    // Broken: called straight from the click handler, on the main thread.
    // Only legal because the database below is opened with
    // allowMainThreadQueries() for this demo.
    @Transaction
    open fun insertBlocking(note: Note): Long = insertHeavy(note)

    // Fixed: the same write, but suspend. Room runs suspend DAO calls on
    // its own executor, off the main thread, no Dispatchers.IO needed.
    @Transaction
    open suspend fun insert(note: Note): Long = insertHeavy(note)

    // A real transaction: the note plus a batch of history rows, padded
    // with a sleep so the total stays a reliable ~8s no matter how fast
    // the device is. Comfortably past the 5s input dispatch timeout that
    // trips the ANR dialog when insertBlocking runs on the main thread.
    private fun insertHeavy(note: Note): Long {
        val start = System.currentTimeMillis()
        val id = insertRaw(note)
        repeat(500) { i -> insertRaw(note.copy(id = 0, body = "${note.body}#$i")) }
        val elapsed = System.currentTimeMillis() - start
        val padding = 8000 - elapsed
        if (padding > 0) Thread.sleep(padding)
        return id
    }
}
