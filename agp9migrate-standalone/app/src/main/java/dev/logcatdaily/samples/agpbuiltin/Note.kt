package dev.logcatdaily.samples.agpbuiltin

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: Long = 0,
    val author: String = "Nishant",
    val text: String,
)
