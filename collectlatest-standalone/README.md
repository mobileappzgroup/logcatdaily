# collectlatest-standalone

A search box as a flow. Three queries are typed 150 ms apart ("k", "ko", "kot") into a shared flow. The search for each one takes 600 ms. The question: what happens to the old searches?

Gradle 9.8.1, Kotlin 2.2.21, coroutines 1.10.2, JDK 21. Plain JVM.

## Symptom

With `collect`, all three searches run to the end, one after the other. The result for "k" is shown at 812 ms, when the box already says "kot". The right result for "kot" arrives at 2089 ms. With `collectLatest`, the old searches stop at the next 100 ms step and "kot" is shown at 1121 ms.

## Trace

From `collect.log` in the post folder (ms since start):

```
149 ms  typed "k"
314 ms  typed "ko"
465 ms  typed "kot"
810 ms  search "k" done
812 ms  shown "results for k"
...
2089 ms  shown "results for kot"
```

Same typing with `collectLatest`:

```
162 ms  search "k" start
320 ms  typed "ko"
323 ms  search "k" cancelled
325 ms  search "ko" start
478 ms  typed "kot"
479 ms  search "ko" cancelled
480 ms  search "kot" start
1121 ms  shown "results for kot"
```

## Culprit

[`Search.kt:41`](src/main/kotlin/Search.kt#L41)

```kotlin
"collect" -> queries.collect { q -> log("shown \"${search(q)}\"") }
```

`collect` runs the block for one value and only then takes the next. Nothing tells the running search that a newer query exists.

## Fix

[`Search.kt:42`](src/main/kotlin/Search.kt#L42)

```kotlin
"collectLatest" -> queries.collectLatest { q -> log("shown \"${search(q)}\"") }
```

`collectLatest` cancels the running block when a new value arrives. A cancel only lands at a suspension point, here `delay`. The `blocking` variant uses `Thread.sleep(600)` instead: it ran all three searches to the end, same as `collect` (`collectlatest-blocking.log`).

## Run it

```
./gradlew --no-daemon run -Pvariant=collect
./gradlew --no-daemon run -Pvariant=collectLatest
./gradlew --no-daemon run -Pvariant=blocking
```
