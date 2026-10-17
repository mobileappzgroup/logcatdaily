package dev.logcatdaily.samples.fileupload

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

// Launch with --es sample file-upload --es variant single | counter | server.
// Anything else is the server variant. --ez autostart true taps Add once,
// --ei chunkKb N overrides the 1024 KB chunk for a quick test.
@Composable
fun FileUploadSample(variant: String?, intent: Intent) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel = remember {
        UploadGraph.configure(activity, variant, intent.getIntExtra("chunkKb", 0))
        ViewModelProvider(activity)[UploadsViewModel::class.java]
    }
    val uploads by viewModel.uploads.collectAsState()
    var started by rememberSaveable { mutableStateOf(false) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        // Only on a fresh launch, so a rotation does not add a second upload.
        if (intent.getBooleanExtra("autostart", false) && !started) {
            started = true
            viewModel.add()
        }
    }

    UploadsScreen(
        modeTag = viewModel.modeTag,
        uploads = uploads,
        onAdd = viewModel::add,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
    )
}
