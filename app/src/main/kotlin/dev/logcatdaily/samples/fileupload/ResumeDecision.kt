package dev.logcatdaily.samples.fileupload

sealed interface Start {
    class From(val offset: Long) : Start
    object Fail : Start
}

object ResumeDecision {
    // counter: trust the row. server: the server's answer wins, even when it is
    // lower than the row, because a server that lost data is right. An offset
    // outside the file means something is wrong and the run stops.
    fun start(
        config: UploadConfig,
        confirmed: Long,
        serverOffset: Long?,
        total: Long
    ): Start {
        val offset =
            if (config.askServerOffset && serverOffset != null)
                serverOffset
            else confirmed
        return if (offset in 0..total) Start.From(offset)
        else Start.Fail
    }
}
