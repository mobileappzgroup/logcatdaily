package dev.logcatdaily.samples.anrroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, NoteDatabase::class.java, "notes.db").build()
    private val dao = db.noteDao()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _savedCount = MutableStateFlow(0)
    val savedCount: StateFlow<Int> = _savedCount.asStateFlow()

    fun save(body: String) {
        if (_saving.value) return
        _saving.value = true
        Log.d(TAG, "save started")
        // Fixed: dao.insert() is a suspend fun, so viewModelScope.launch is
        // enough on its own. Room already keeps this off the main thread.
        viewModelScope.launch {
            dao.insert(Note(body = body))
            Log.d(TAG, "save finished")
            _saving.value = false
            _savedCount.value++
        }
    }
}
