package dev.logcatdaily.samples.fileupload

import java.io.File
import java.security.MessageDigest
import java.util.Random

// The mock file: 24 MiB of seeded pseudo-random bytes, written once. Same bytes
// every run, so the hash on the phone and the hash on the server can be compared.
object TestFile {
    const val NAME = "nishant-trip.mp4"
    const val SIZE = 24L * 1024 * 1024
    private const val SEED = 24L
    private const val STEP = 64 * 1024

    fun ensure(dir: File): File {
        dir.mkdirs()
        val file = File(dir, NAME)
        if (file.length() == SIZE) return file
        val random = Random(SEED)
        val buffer = ByteArray(STEP)
        // Write to a temp name first so a killed run never leaves a short file.
        val temp = File(dir, "$NAME.part")
        temp.outputStream().buffered().use { out ->
            var left = SIZE
            while (left > 0) {
                random.nextBytes(buffer)
                val n = minOf(left, STEP.toLong()).toInt()
                out.write(buffer, 0, n)
                left -= n
            }
        }
        check(temp.renameTo(file)) { "could not move ${temp.name} to ${file.name}" }
        return file
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(STEP)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
