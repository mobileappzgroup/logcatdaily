package dev.logcatdaily.samples.offlinechat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatGraph.repository(application)

    // The screen only reads Room, through this flow. Status changes from the
    // worker show up here without the screen knowing a worker exists.
    val messages: StateFlow<List<Message>> = repository.observeMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sendWithoutKey = MutableStateFlow(repository.sendWithoutKey)
    val sendWithoutKey: StateFlow<Boolean> = _sendWithoutKey.asStateFlow()

    init {
        viewModelScope.launch {
            repository.resumePending()
            repository.sync()
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.send(trimmed) }
    }

    fun retry(clientId: String) {
        viewModelScope.launch { repository.retry(clientId) }
    }

    fun setSendWithoutKey(enabled: Boolean) {
        repository.sendWithoutKey = enabled
        _sendWithoutKey.value = enabled
    }
}
