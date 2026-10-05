package dev.logcatdaily.samples.wideparam

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

private data class UiState(val name: String, val unread: Int, val taps: Int)

@Composable
fun WideParamSample(broken: Boolean) {
    var state by remember { mutableStateOf(UiState("Asha", unread = 3, taps = 0)) }
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "Screen recompose #${++count[0]}") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Button(
            onClick = { state = state.copy(taps = state.taps + 1) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("taps: ${state.taps}", fontSize = 20.sp)
        }
        // the card only shows the name
        if (broken) NameCard(state) else NameCard(state.name)
    }
}

// takes the whole state, so any field change makes it recompose
@Composable
private fun NameCard(state: UiState) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "NameCard recompose #${++count[0]}") }
    NameText(state.name)
}

@Composable
private fun NameCard(name: String) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "NameCard recompose #${++count[0]}") }
    NameText(name)
}

@Composable
private fun NameText(name: String) {
    Text(
        text = name,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 32.dp),
    )
}
