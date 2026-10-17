package dev.logcatdaily.samples.fileupload

class Chunk(val offset: Long, val length: Int)

object ChunkPlan {
    // The slice that starts at offset. It ends at the next chunk boundary or at
    // the end of the file, so a server that kept half a chunk is still handled.
    // Null when there is nothing left to send.
    fun next(offset: Long, total: Long, chunkBytes: Int): Chunk? {
        if (offset < 0 || offset >= total) return null
        val toBoundary = chunkBytes - offset % chunkBytes
        return Chunk(offset, minOf(toBoundary, total - offset).toInt())
    }
}
