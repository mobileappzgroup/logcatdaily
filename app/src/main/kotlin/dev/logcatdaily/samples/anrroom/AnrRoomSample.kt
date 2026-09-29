package dev.logcatdaily.samples.anrroom

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
private const val DRAFT = "Renew the library card"

private data class NoteItem(val title: String, val body: String)

// Shown on launch. Display only, the database is not seeded.
private val seedNotes = listOf(
    NoteItem("Groceries", "Milk, eggs, bread, coffee filters"),
    NoteItem("Team sync", "Thursday 10:30, bring the sprint board"),
    NoteItem("Book to read", "The sci-fi novel from the library"),
    NoteItem("Dentist", "Friday 4 pm, ask about the night guard"),
    NoteItem("Gym plan", "Legs on Monday, swimming on Wednesday"),
    NoteItem("Birthday gift", "Something for Mum, maybe a plant"),
    NoteItem("Packing list", "Charger, rain jacket, sunscreen, passport"),
    NoteItem("Water bill", "Due on the 15th, pay from the app"),
)

@Composable
fun AnrRoomSample(broken: Boolean) {
    NotesTheme {
        if (broken) {
            BrokenNoteScreen()
        } else {
            FixedNoteScreen()
        }
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

    NotesScreen(saving = saving, savedCount = savedCount, onSaveClick = ::onSaveClick)
}

@Composable
private fun FixedNoteScreen() {
    val context = LocalContext.current
    val viewModel = remember { NotesViewModel(context.applicationContext as Application) }
    val saving by viewModel.saving.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()

    NotesScreen(saving = saving, savedCount = savedCount, onSaveClick = { viewModel.save(NOTE_BODY) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreen(saving: Boolean, savedCount: Int, onSaveClick: () -> Unit) {
    val notes = remember { mutableStateListOf(*seedNotes.toTypedArray()) }
    var draft by rememberSaveable { mutableStateOf(DRAFT) }
    val listState = rememberLazyListState()

    // Each finished save puts the note at the top and clears the field.
    LaunchedEffect(savedCount) {
        if (savedCount > 0) {
            notes.add(0, NoteItem(draft.ifBlank { "Untitled" }, "Just now"))
            draft = ""
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            Composer(
                draft = draft,
                onDraftChange = { draft = it },
                saving = saving,
                saved = savedCount > 0 && !saving,
                onSaveClick = onSaveClick,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(notes) { NoteRow(it) }
        }
    }
}

@Composable
private fun NoteRow(note: NoteItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = note.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.body,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    saving: Boolean,
    saved: Boolean,
    onSaveClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                placeholder = { Text("Write a note", fontSize = 18.sp) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                } else if (saved) {
                    Text(
                        text = "Saved",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.width(150.dp).height(56.dp),
                ) {
                    Text("Save", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
