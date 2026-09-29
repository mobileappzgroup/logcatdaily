package dev.logcatdaily.samples.anrroom

import android.os.StrictMode

// Example only. Nothing calls this function, it is not wired into the sample.
// In a real app, call it from Application.onCreate() in debug builds.
fun installDebugStrictMode() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
}
