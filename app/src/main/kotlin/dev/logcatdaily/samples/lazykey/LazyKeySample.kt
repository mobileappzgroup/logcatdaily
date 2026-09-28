package dev.logcatdaily.samples.lazykey

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val TAG = "logcatdaily"

private data class Task(val id: Int, val label: String)

private fun initialTasks() = listOf(
    Task(1, "Fix crash on cold start"),
    Task(2, "Update compileSdk to 36"),
    Task(3, "Review PR #42"),
    Task(4, "Reply to Play Store review"),
    Task(5, "Write release notes"),
    Task(6, "Bump target API"),
    Task(7, "Triage ANR reports"),
    Task(8, "Ship dark mode"),
)

@Composable
fun LazyKeySample(broken: Boolean) {
    var tasks by remember { mutableStateOf(initialTasks()) }

    fun deleteTask(id: Int) {
        tasks = tasks.filterNot { it.id == id }
    }

    // just a live dot so the screen keeps redrawing even when nothing else
    // on it is changing (screen recordings otherwise stop advancing during
    // a fully static hold)
    var pulseOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            pulseOn = !pulseOn
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tasks",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (pulseOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                    )
                    TextButton(onClick = { tasks = tasks.shuffled() }) {
                        Text("Shuffle", fontSize = 18.sp)
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Broken: no key, so each slot's remembered checkbox state
                // stays put by position when the list reorders. Fixed: keying
                // on the task id moves the state with the task instead.
                if (broken) {
                    items(tasks) { task -> TaskRow(task, onDelete = { deleteTask(task.id) }) }
                } else {
                    items(tasks, key = { it.id }) { task -> TaskRow(task, onDelete = { deleteTask(task.id) }) }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onDelete: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Log.d(TAG, "composing item id=${task.id} label=\"${task.label}\" checked=$checked")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Text(
            text = task.label,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
