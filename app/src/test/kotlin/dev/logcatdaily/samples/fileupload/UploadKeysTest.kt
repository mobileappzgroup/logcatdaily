package dev.logcatdaily.samples.fileupload

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UploadKeysTest {

    @Test
    fun `the key is the row's and is the same after a restart`() = runBlocking {
        val file = File.createTempFile("upload", ".bin").also { it.writeBytes(ByteArray(2_500) { b -> b.toByte() }) }
        try {
            val dao = FakeUploadDao()
            val api = FakeUploadApi()
            dao.insert(
                UploadEntity("u-0001", "7c1e-key", "nishant-trip.mp4", file.absolutePath, file.length(), TestFile.sha256(file), createdAt = 1),
            )
            api.dropCreateReply = true
            val config = UploadConfig.SERVER.copy(chunkBytes = 1000)
            UploadRunner(dao, api, config).run("u-0001")
            // A new runner stands for a new process: it only has the row.
            UploadRunner(FakeUploadDao().also { it.insert(dao.get("u-0001")!!) }, api, config).run("u-0001")
            assertEquals(listOf<String?>("7c1e-key", "7c1e-key"), api.createKeys)
        } finally {
            file.delete()
        }
    }
}
