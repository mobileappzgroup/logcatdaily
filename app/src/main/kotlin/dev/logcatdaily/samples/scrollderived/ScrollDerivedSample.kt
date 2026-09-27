package dev.logcatdaily.samples.scrollderived

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val avatarInitials = listOf("AK", "JR", "MN", "TS", "QW", "ZX", "LB", "YP", "CV", "GD")

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
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Inbox",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(100) { index -> InboxRow(index) }
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

@Composable
private fun InboxRow(index: Int) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatarInitials[index % avatarInitials.size],
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Item $index",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Updated ${index + 1}m ago",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}
