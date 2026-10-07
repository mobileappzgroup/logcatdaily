# cancel-swallow

A 6-step "upload" that ticks every 500 ms, with a Cancel button. Tap Cancel
in the broken version and it still reports "upload finished".

## Symptom

The screen reads "step N of 6". I tapped Cancel after step 4. Instead of
stopping, steps 5 and 6 were skipped through in the same millisecond and the
log ended with `upload finished`, as if nothing had been cancelled.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: upload started
logcatdaily: tick 1/6
...
logcatdaily: tick 4/6
logcatdaily: cancel requested
logcatdaily: caught JobCancellationException on step 5, retrying anyway
logcatdaily: caught JobCancellationException on step 6, retrying anyway
logcatdaily: upload finished
```

`cancel requested` and the three lines after it share the timestamp 3.775 s. Same steps on
the fixed variant:

```
logcatdaily: upload started
logcatdaily: tick 1/6
...
logcatdaily: tick 3/6
logcatdaily: cancel requested
```

Nothing is logged after the cancel, and `upload finished` never appears.

## Culprit

[`CancelSwallowViewModel.kt:38`](CancelSwallowViewModel.kt#L38)

```kotlin
} catch (e: Exception) {
```

`delay(500)` throws a `CancellationException` when the job is cancelled.
`catch (e: Exception)` catches it, the handler only logs and carries on, and
the `while` loop starts the next step. Each later `delay` throws at once, so
the remaining steps burn through instantly. A narrower catch only helps if it
is not a parent of `CancellationException`: `IllegalStateException` and
`RuntimeException` still catch it.

## Fix

[`CancelSwallowViewModel.kt:41`](CancelSwallowViewModel.kt#L41)

```kotlin
if (!broken && e is CancellationException) throw e
```

Rethrowing it lets the cancellation travel up and end the coroutine. The
`!broken` is the sample's switch, in real code it is just
`if (e is CancellationException) throw e`.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-swallow --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-swallow --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Long: https://youtu.be/q60oKHK_0m0
