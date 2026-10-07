# cancel-loop

A screen that counts to 20 on a background loop, with a Cancel button. Tap
Cancel in the broken version and the counter keeps going.

## Symptom

The screen reads "tick N of 20". I tapped Cancel after tick 7 and the count
kept climbing to 20 on its own, 13 more ticks. The loop exited 3.773 s after
`cancel requested`.

## Trace

From the recording, tag `logcatdaily`, broken variant (times are the first
column of the capture):

```
logcatdaily: loop started
logcatdaily: tick 1/20
...
logcatdaily: tick 7/20
logcatdaily: cancel requested
logcatdaily: tick 8/20
...
logcatdaily: tick 20/20
logcatdaily: loop exited at tick 20
```

`cancel requested` at 3.750 s, `loop exited at tick 20` at 7.523 s. Same steps
on the fixed variant:

```
logcatdaily: tick 7/20
logcatdaily: cancel requested
logcatdaily: isActive false, stopping at tick 7
logcatdaily: loop exited at tick 7
```

The loop stops 2 ms after the cancel.

## Culprit

[`CancelLoopViewModel.kt:33`](CancelLoopViewModel.kt#L33)

```kotlin
while (tick < MAX_TICKS) {
```

The loop runs on `Dispatchers.Default` with no suspend call inside it, so
there is no point where the coroutine could notice `cancel()`. Cancellation
is cooperative: it only sets the job to cancelling, and the loop has to look.
Nothing looks, so it runs until its own cap of 20. In the sample a `broken`
flag turns the check off so one app can show both runs. In real code the bug
is just the missing check.

## Fix

[`CancelLoopViewModel.kt:34`](CancelLoopViewModel.kt#L34)

```kotlin
if (!broken && !isActive) {
    Log.d(TAG, "isActive false, stopping at tick $tick")
    break
}
```

`isActive` goes false as soon as the job is cancelled, and the check runs on
every pass, so the loop sees it on the next spin. `ensureActive()` does the
same check but throws a `CancellationException` instead of returning a flag,
and `yield()` also checks, but it suspends and gives other work a turn.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-loop --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample cancel-loop --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Long: https://youtu.be/q60oKHK_0m0
