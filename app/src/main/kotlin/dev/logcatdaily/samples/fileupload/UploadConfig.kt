package dev.logcatdaily.samples.fileupload

// One runner, three presets. Each variant flips only what it adds, so the code
// diff between two variants is the flag.
data class UploadConfig(
    val variant: String,
    val modeTag: String,
    val chunked: Boolean,
    val askServerOffset: Boolean,
    val idempotentCreate: Boolean,
    val finalCheck: Boolean,
    val chunkBytes: Int = 1_048_576,
    val maxAttempts: Int = 6,
) {
    companion object {
        val SINGLE = UploadConfig("single", "single: one request", chunked = false, askServerOffset = false, idempotentCreate = false, finalCheck = false)
        val COUNTER = UploadConfig("counter", "chunks: my counter", chunked = true, askServerOffset = false, idempotentCreate = false, finalCheck = true)
        val SERVER = UploadConfig("server", "chunks: server offset", chunked = true, askServerOffset = true, idempotentCreate = true, finalCheck = true)

        // Anything else is the server variant.
        fun forVariant(variant: String?): UploadConfig = when (variant) {
            "single" -> SINGLE
            "counter" -> COUNTER
            else -> SERVER
        }
    }
}
