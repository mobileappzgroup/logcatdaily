package dev.logcatdaily.samples.fileupload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadsScreen(
    modeTag: String,
    uploads: List<UploadEntity>,
    onAdd: () -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Uploads", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(modeTag, fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onAdd, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Add 24 MB file", fontSize = 18.sp)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uploads, key = { it.id }) { UploadRow(it, onCancel, onRetry) }
            }
        }
    }
}

@Composable
private fun UploadRow(upload: UploadEntity, onCancel: (String) -> Unit, onRetry: (String) -> Unit) {
    val active = upload.state in setOf(UploadState.QUEUED, UploadState.UPLOADING, UploadState.WAITING)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(upload.fileName, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { upload.confirmedBytes.toFloat() / upload.totalBytes.coerceAtLeast(1) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(progressText(upload.confirmedBytes, upload.totalBytes), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stateText(upload),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                if (active) OutlinedButton(onClick = { onCancel(upload.id) }) { Text("Cancel") }
                if (upload.state == UploadState.FAILED) OutlinedButton(onClick = { onRetry(upload.id) }) { Text("Retry") }
            }
        }
    }
}
