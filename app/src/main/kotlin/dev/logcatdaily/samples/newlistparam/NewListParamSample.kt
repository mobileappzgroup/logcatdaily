package dev.logcatdaily.samples.newlistparam

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

private data class Task(val id: Int, val title: String, val done: Boolean)

@Composable
fun NewListParamSample(broken: Boolean) {
    val tasks = remember { List(30) { Task(it, "Task $it", done = it % 3 == 0) } }
    var taps by remember { mutableIntStateOf(0) }
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "Screen recompose #${++count[0]}") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Button(
            onClick = { taps++ },
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        ) {
            Text("add tap", fontSize = 20.sp)
        }
        Text(
            text = "taps: $taps",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        // filter runs on every recomposition of this screen, not just when tasks change
        val open = if (broken) {
            tasks.filter { !it.done }
        } else {
            remember(tasks) { tasks.filter { !it.done } }
        }
        TaskList(open)
    }
}

@Composable
private fun TaskList(tasks: List<Task>) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "TaskList recompose #${++count[0]}") }

    LazyColumn(Modifier.fillMaxSize()) {
        items(tasks, key = { it.id }) { TaskRow(it) }
    }
}

@Composable
private fun TaskRow(task: Task) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "TaskRow ${task.id} recompose #${++count[0]}") }

    Text(
        text = task.title,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
    )
}
