package dev.logcatdaily.samples.effectkey

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val TAG = "logcatdaily"

private data class ProfileUiState(val userId: String, val loadedAt: Long)

@Composable
fun EffectKeySample(broken: Boolean) {
    val userId = "user-42"
    var uiState by remember { mutableStateOf(ProfileUiState(userId = userId, loadedAt = 0L)) }
    var loadCount by remember { mutableStateOf(0) }

    // just a live dot so the screen keeps redrawing even once loading settles
    var pulseOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            pulseOn = !pulseOn
        }
    }

    // Broken: the effect is keyed on uiState itself, and the effect body writes a
    // new uiState (with a fresh loadedAt) when the load finishes. That new value
    // is a new key, so the effect restarts itself, forever, with no user input.
    // Fixed: key on userId, which does not change once the screen loads.
    if (broken) {
        LaunchedEffect(uiState) {
            loadCount++
            Log.d(TAG, "effect started #$loadCount")
            try {
                delay(700)
                uiState = uiState.copy(loadedAt = System.currentTimeMillis())
                Log.d(TAG, "loaded #$loadCount")
            } catch (e: CancellationException) {
                Log.d(TAG, "effect cancelled #$loadCount")
                throw e
            }
        }
    } else {
        LaunchedEffect(userId) {
            loadCount++
            Log.d(TAG, "effect started #$loadCount")
            try {
                delay(700)
                uiState = uiState.copy(loadedAt = System.currentTimeMillis())
                Log.d(TAG, "loaded #$loadCount")
            } catch (e: CancellationException) {
                Log.d(TAG, "effect cancelled #$loadCount")
                throw e
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "user: $userId",
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "loads triggered: $loadCount",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .size(14.dp)
                .background(
                    color = if (pulseOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                )
        )
    }
}
