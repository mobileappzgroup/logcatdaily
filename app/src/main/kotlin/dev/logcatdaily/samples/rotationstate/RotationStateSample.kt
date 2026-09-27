package dev.logcatdaily.samples.rotationstate

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

@Composable
fun RotationStateSample(broken: Boolean) {
    // Broken: remember alone forgets the value across a config change (rotation
    // recreates the activity by default). Fixed: rememberSaveable survives it.
    val countState = if (broken) {
        remember { mutableStateOf(0) }
    } else {
        rememberSaveable { mutableStateOf(0) }
    }
    var count by countState

    Log.d(TAG, "composing with count=$count")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "count: $count",
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { count++ }) {
                Text("tap to increment", fontSize = 20.sp)
            }
        }
    }
}
