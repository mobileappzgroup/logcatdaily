package dev.logcatdaily.samples.cancelloop

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"
private const val MAX_TICKS = 20
private const val TICK_MS = 300L

// A CPU-bound loop with no suspend call inside it, so there is no natural
// cancellation point. Broken never checks isActive, so cancel() has nothing
// to interrupt: the loop only stops when it hits its own safety cap. Fixed
// checks isActive every pass, so cancel() is noticed on the very next spin.
class CancelLoopViewModel(private val broken: Boolean) : ViewModel() {
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick.asStateFlow()

    private var job: Job? = null

    fun start() {
        Log.d(TAG, "loop started")
        job = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0
            var lastLogMs = System.currentTimeMillis()
            while (tick < MAX_TICKS) {
                if (!broken && !isActive) {
                    Log.d(TAG, "isActive false, stopping at tick $tick")
                    break
                }
                val now = System.currentTimeMillis()
                if (now - lastLogMs >= TICK_MS) {
                    lastLogMs = now
                    tick++
                    _tick.value = tick
                    Log.d(TAG, "tick $tick/$MAX_TICKS")
                }
            }
            Log.d(TAG, "loop exited at tick $tick")
        }
    }

    fun cancel() {
        Log.d(TAG, "cancel requested")
        job?.cancel()
    }
}
