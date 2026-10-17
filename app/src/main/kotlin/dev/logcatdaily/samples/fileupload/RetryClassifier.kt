package dev.logcatdaily.samples.fileupload

enum class Call { CREATE, HEAD, CHUNK, LAST_CHUNK, WHOLE }

sealed interface Action {
    // Network gone or server busy: WAITING, WorkManager backs off.
    object Retry : Action
    // 409 with askServerOffset: HEAD, take its offset, go on at once.
    object AskServer : Action
    // 409 without it: a failed attempt, backoff, no HEAD. This is the bug.
    object CountAttempt : Action
    class Fail(val error: String) : Action
}

object RetryClassifier {
    // reply null means the call threw IOException (reset, timeout, no route).
    fun classify(config: UploadConfig, call: Call, reply: ApiReply?): Action = when (reply) {
        null -> Action.Retry
        is ApiReply.ServerError -> Action.Retry
        is ApiReply.Conflict -> if (config.askServerOffset) Action.AskServer else Action.CountAttempt
        is ApiReply.Gone -> if (call == Call.HEAD || call == Call.CHUNK || call == Call.LAST_CHUNK) Action.Fail("session gone") else Action.Fail("HTTP ${reply.code}")
        is ApiReply.Rejected ->
            if (reply.code == 422 && call == Call.LAST_CHUNK) Action.Fail("checksum differs") else Action.Fail("HTTP ${reply.code}")
        is ApiReply.Ok -> error("a successful reply is not classified")
    }
}
