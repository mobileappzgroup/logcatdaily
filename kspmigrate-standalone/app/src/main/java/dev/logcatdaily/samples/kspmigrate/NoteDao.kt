package dev.logcatdaily.samples.kspmigrate

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Query("SELECT * FROM notes WHERE author = :author")
    suspend fun byAuthor(author: String): List<Note>
}
