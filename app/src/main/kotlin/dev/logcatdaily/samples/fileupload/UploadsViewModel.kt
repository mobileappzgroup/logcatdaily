package dev.logcatdaily.samples.fileupload

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UploadGraph.repository(application)

    val modeTag: String = UploadGraph.config(application).modeTag

    // The screen reads Room only, through this flow.
    val uploads: StateFlow<List<UploadEntity>> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add() {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { TestFile.ensure(File(getApplication<Application>().filesDir, "uploads")) }
            repository.enqueue(file)
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch { repository.cancel(id) }
    }

    fun retry(id: String) {
        viewModelScope.launch { repository.retry(id) }
    }
}
