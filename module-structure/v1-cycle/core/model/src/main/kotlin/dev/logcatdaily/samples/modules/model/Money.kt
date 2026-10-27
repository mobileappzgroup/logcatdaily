package dev.logcatdaily.samples.modules.model

data class Money(val cents: Long) {
    fun format(): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
