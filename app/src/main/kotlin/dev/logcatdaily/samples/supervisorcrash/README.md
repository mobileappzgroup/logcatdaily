# supervisor-crash

A Jobs screen with a Run button. It starts two coroutines: one throws, one finishes after 500 ms. The scope has a SupervisorJob, and the app still crashes.

## Symptom

Tap Run. The app closes to the launcher within half a second. No sibling result ever shows.
With the fix, the app stays open and the card reads `Jobs: sibling done`.

## Trace

From a real run (API 36 emulator), broken variant, `adb logcat`:

```
E AndroidRuntime: FATAL EXCEPTION: DefaultDispatcher-worker-1
E AndroidRuntime: java.lang.IllegalStateException: boom
E AndroidRuntime: 	at dev.logcatdaily.samples.supervisorcrash.SupervisorCrashKt$startJobs$1.invokeSuspend(SupervisorCrash.kt:19)
...
I Process : Sending signal. PID: 13135 SIG: 9
```

`sibling done` never appears in the full log of this run. Same tap on the fixed variant, `adb logcat -s logcatdaily`:

```
D logcatdaily: caught java.lang.IllegalStateException: boom
D logcatdaily: sibling done
```

The two lines are 0.511 s apart, and the process keeps running.

## Culprit

[`SupervisorCrash.kt:15`](SupervisorCrash.kt#L15)

```kotlin
fun brokenScope() = CoroutineScope(SupervisorJob())
```

A SupervisorJob stops one child's failure from cancelling its siblings. It does not catch anything. The exception from `error("boom")` is unhandled, so it reaches the thread's uncaught exception handler, and on Android that ends the app.

## Fix

[`SupervisorCrash.kt:28`](SupervisorCrash.kt#L28)

```kotlin
fun fixedScope() = CoroutineScope(SupervisorJob() + handler)
```

The `CoroutineExceptionHandler` is in the scope, and the coroutines are launched straight from that scope. A handler on a child launched inside another coroutine is ignored. The handler cannot recover or retry, the coroutine is already done when it runs. I would put a try/catch around the risky call first and keep this as a last-resort logger.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample supervisor-crash --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample supervisor-crash --es variant fixed
adb logcat
```

## Video

- Short: https://youtube.com/shorts/pOP8Zq2OkqI
- Reel: https://www.instagram.com/reel/De1uW7OgQyP/
