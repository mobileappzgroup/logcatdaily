package dev.logcatdaily.samples.scrollderived

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

@Composable
fun ScrollDerivedSample(broken: Boolean) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val recomposeCount = remember { intArrayOf(0) }

    // Broken: reading firstVisibleItemIndex directly in the composable subscribes
    // this whole function to that snapshot state, so it recomposes every time the
    // index changes, not just when the derived boolean actually flips.
    // Fixed: derivedStateOf only notifies readers when the output value changes.
    val showButton = if (broken) {
        listState.firstVisibleItemIndex > 0
    } else {
        val derived by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
        derived
    }

    SideEffect {
        recomposeCount[0]++
        Log.d(TAG, "recompose #${recomposeCount[0]}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 24.dp),
        ) {
            items(100) { index ->
                Text(
                    text = "row $index",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (showButton) {
            Button(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Text("scroll to top", fontSize = 20.sp)
            }
        }
    }
}
