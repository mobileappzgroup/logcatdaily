import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val t0 = System.nanoTime()
private fun log(msg: String) = println("%5d ms  %s".format((System.nanoTime() - t0) / 1_000_000, msg))

// Slow search: 600 ms of work, checked in 100 ms steps.
private suspend fun search(q: String): String {
    log("search \"$q\" start")
    try {
        repeat(6) { delay(100) }
    } catch (e: CancellationException) {
        log("search \"$q\" cancelled")
        throw e
    }
    log("search \"$q\" done")
    return "results for $q"
}

// Same 600 ms, but nothing in it can notice a cancel.
private suspend fun searchBlocking(q: String): String {
    log("search \"$q\" start")
    Thread.sleep(600)
    log("search \"$q\" done")
    return "results for $q"
}

fun main(args: Array<String>) = runBlocking {
    val variant = args.firstOrNull() ?: "collect"
    val queries = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val typing = launch {
        delay(100) // let the collector subscribe
        for (q in listOf("k", "ko", "kot")) {
            log("typed \"$q\"")
            queries.emit(q)
            delay(150)
        }
    }
    val collector = launch(Dispatchers.Default) {
        when (variant) {
            "collect" -> queries.collect { q -> log("shown \"${search(q)}\"") }
            "collectLatest" -> queries.collectLatest { q -> log("shown \"${search(q)}\"") }
            "blocking" -> queries.collectLatest { q -> log("shown \"${searchBlocking(q)}\"") }
        }
    }
    typing.join()
    delay(2500)
    collector.cancel()
    log("end")
}
