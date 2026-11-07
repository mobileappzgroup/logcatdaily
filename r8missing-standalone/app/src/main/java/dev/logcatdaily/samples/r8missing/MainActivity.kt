package dev.logcatdaily.samples.r8missing

import android.app.Activity
import android.os.Bundle
import dev.logcatdaily.samples.r8missing.lib.Report

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Report("Nishant").send()
    }
}
