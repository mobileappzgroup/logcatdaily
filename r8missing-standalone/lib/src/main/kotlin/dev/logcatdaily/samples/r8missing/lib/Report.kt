package dev.logcatdaily.samples.r8missing.lib

import org.slf4j.LoggerFactory

class Report(private val name: String) {
    fun send() {
        LoggerFactory.getLogger("report").info("sent for $name")
    }
}
