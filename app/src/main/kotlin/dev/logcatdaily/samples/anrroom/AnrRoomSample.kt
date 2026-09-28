package dev.logcatdaily.samples.anrroom

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room

private const val TAG = "logcatdaily"
private const val NOTE_BODY = "milk, eggs, bread, the good coffee filters"

@Composable
fun AnrRoomSample(broken: Boolean) {
    if (broken) {
        BrokenNoteScreen()
    } else {
        FixedNoteScreen()
    }
}

@Composable
private fun BrokenNoteScreen() {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(context, NoteDatabase::class.java, "notes.db")
            .allowMainThreadQueries()
            .build()
    }
    var saving by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }

    fun onSaveClick() {
        if (saving) return
        saving = true
        Log.d(TAG, "save started")
        // Broken: the write happens right here, on the main thread, inside
        // the click handler. allowMainThreadQueries() is what lets this
        // compile instead of Room throwing.
        db.noteDao().insertBlocking(Note(body = NOTE_BODY))
        Log.d(TAG, "save finished")
        saving = false
        savedCount++
    }

    NoteCard(saving = saving, savedCount = savedCount, onSaveClick = ::onSaveClick)
}

@Composable
private fun FixedNoteScreen() {
    val context = LocalContext.current
    val viewModel = remember { NotesViewModel(context.applicationContext as Application) }
    val saving by viewModel.saving.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()

    NoteCard(saving = saving, savedCount = savedCount, onSaveClick = { viewModel.save(NOTE_BODY) })
}

@Composable
private fun NoteCard(saving: Boolean, savedCount: Int, onSaveClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
        ) {
            Text(
                text = "New note",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = NOTE_BODY,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSaveClick) {
                    Text("Save", fontSize = 20.sp)
                }
                if (saving) {
                    Spacer(Modifier.width(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            if (savedCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "saved $savedCount time(s)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
