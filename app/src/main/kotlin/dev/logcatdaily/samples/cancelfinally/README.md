# cancel-finally

A 5-step "saving draft" job with a Cancel button and a cleanup step in
`finally`. Tap Cancel in the broken version and the cleanup starts but never
finishes.

## Symptom

The screen reads "working N/5". I tapped Cancel after step 2. The job stops
working, but the log shows `cleanup started` as its last line. The cleanup
never reports done.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: job started
logcatdaily: tick 1/5
logcatdaily: tick 2/5
logcatdaily: cancel requested
logcatdaily: cleanup started
```

The capture ends there. Same steps on the fixed variant:

```
logcatdaily: job started
logcatdaily: tick 1/5
logcatdaily: tick 2/5
logcatdaily: cancel requested
logcatdaily: cleanup started
logcatdaily: cleanup done
```

`cleanup done` lands 501 ms after `cleanup started` (2.599 s to 3.100 s).

## Culprit

[`CancelFinallyViewModel.kt:41`](CancelFinallyViewModel.kt#L41)

```kotlin
cleanup()
```

By the time `finally` runs, the job is already cancelling. `cleanup()`
suspends, and its `delay(500)` throws a `CancellationException` right away in
a cancelling job, so the function ends after `cleanup started`. The `delay`
stands in for real suspend cleanup like flushing a buffer or closing a file.

## Fix

[`CancelFinallyViewModel.kt:43`](CancelFinallyViewModel.kt#L43)

```kotlin
withContext(NonCancellable) { cleanup() }
```

`withContext(NonCancellable)` runs the block under a job that is always
active, so a cancellable suspension point like `delay` inside it no longer
throws and the cleanup runs to the end. You only need it when the cleanup
suspends. A plain non-suspending call in `finally` (closing a file, writing a
log line) runs fine without it. It is the standard fix, not the only one.
Wrap only the cleanup. Running the whole job in `NonCancellable` would make
`cancel()` do nothing, and keep the cleanup short, because `cancel()` waits
for it.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-finally --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-finally --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Long: https://youtu.be/q60oKHK_0m0
