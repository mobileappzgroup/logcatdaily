package dev.logcatdaily.samples.cancelswallow

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"
private const val TOTAL_STEPS = 6

// A 6-step "upload" that ticks every 500ms. Broken wraps each step in
// catch (e: Exception), the usual "keep retrying on any failure" mistake,
// which also catches the CancellationException the screen sends when it
// goes away. So the loop treats "you got cancelled" the same as a network
// hiccup and just moves on to the next step instead of stopping.
class CancelSwallowViewModel(private val broken: Boolean) : ViewModel() {
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private var job: Job? = null

    fun start() {
        Log.d(TAG, "upload started")
        job = viewModelScope.launch {
            var n = 0
            while (n < TOTAL_STEPS) {
                n++
                try {
                    delay(500)
                    _step.value = n
                    Log.d(TAG, "tick $n/$TOTAL_STEPS")
                } catch (e: Exception) {
                    // Fixed: a real CancellationException must be rethrown, never
                    // swallowed, or structured concurrency breaks.
                    if (!broken && e is CancellationException) throw e
                    Log.d(TAG, "caught ${e::class.simpleName} on step $n, retrying anyway")
                }
            }
            Log.d(TAG, "upload finished")
        }
    }

    fun cancel() {
        Log.d(TAG, "cancel requested")
        job?.cancel()
    }
}
