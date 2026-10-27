package dev.logcatdaily.samples.datacopy

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

// Broken: the tags field is a MutableList. copy() copies the reference, not the list.
data class BrokenNote(val owner: String, val tags: MutableList<String>)

// Fixed: the tags field is a read-only List. A change builds a new list.
data class FixedNote(
    val owner: String,
    val tags: List<String>)

private fun id(list: Any) = Integer.toHexString(System.identityHashCode(list))

@Composable
fun DataCopySample(broken: Boolean) {
    // Compose state holds a version counter so the cards redraw after an in-place change.
    var version by remember { mutableStateOf(0) }

    if (broken) {
        val original = remember { BrokenNote("Nishant", mutableListOf("android")) }
        val draft = remember { original.copy() }
        remember { Log.d(TAG, "copy: original list=${id(original.tags)} draft list=${id(draft.tags)}") }
        Screen(
            version, original.owner, original.tags.joinToString(", "), draft.tags.joinToString(", "),
        ) {
            draft.tags.add("kotlin")
            version++
            Log.d(TAG, "draft.tags.add: original=${original.tags} draft=${draft.tags} ids ${id(original.tags)} ${id(draft.tags)}")
        }
    } else {
        var original by remember { mutableStateOf(FixedNote("Nishant", listOf("android"))) }
        var draft by remember { mutableStateOf(original.copy()) }
        remember { Log.d(TAG, "copy: original list=${id(original.tags)} draft list=${id(draft.tags)}") }
        Screen(
            version, original.owner, original.tags.joinToString(", "), draft.tags.joinToString(", "),
        ) {
    draft = draft.copy(
        tags = draft.tags +
            "kotlin")
            version++
            Log.d(TAG, "draft.copy(tags + kotlin): original=${original.tags} draft=${draft.tags} ids ${id(original.tags)} ${id(draft.tags)}")
        }
    }
}

@Composable
private fun Screen(
    @Suppress("UNUSED_PARAMETER") version: Int,
    owner: String,
    original: String,
    draft: String,
    onAdd: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Note by $owner", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Original", fontSize = 26.sp, modifier = Modifier.padding(top = 32.dp))
        Text(original, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("Draft (copy)", fontSize = 26.sp, modifier = Modifier.padding(top = 32.dp))
        Text(draft, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = onAdd,
            modifier = Modifier.padding(top = 40.dp).fillMaxWidth().height(88.dp),
        ) { Text("Add tag to draft", fontSize = 28.sp) }
    }
}
