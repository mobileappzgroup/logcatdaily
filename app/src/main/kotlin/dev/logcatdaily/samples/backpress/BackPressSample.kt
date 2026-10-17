package dev.logcatdaily.samples.backpress

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logcatdaily.samples.ui.Avatar

private const val TAG = "logcatdaily"

@Composable
fun BackPressSample(
    broken: Boolean,
    legacyDialog: Boolean,
    onLegacyDialogDismiss: () -> Unit,
    onEditsChanged: (Int) -> Unit = {},
) {
    var edits by remember { mutableIntStateOf(0) }
    var fixedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(edits) { onEditsChanged(edits) }

    if (!broken) {
        BackHandler(enabled = edits > 0) {
            Log.d(TAG, "BackHandler invoked")
            fixedDialog = true
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar("Nishant", size = 44.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Draft",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Nishant · Release notes v2.4 · 10:05",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "$edits unsaved edits",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Fixes a crash on cold start and bumps targetSdk to 36.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    edits++
                    Log.d(TAG, "edits=$edits")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit", fontSize = 18.sp)
            }
        }
    }

    if (legacyDialog || fixedDialog) {
        val dismiss = {
            fixedDialog = false
            onLegacyDialogDismiss()
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Discard $edits changes?") },
            confirmButton = {
                TextButton(onClick = {
                    edits = 0
                    dismiss()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text("Keep editing") }
            },
        )
    }
}
