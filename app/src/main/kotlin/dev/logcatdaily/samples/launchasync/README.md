# launch-async

A status card with three buttons: `launch`, `async, no await` and `async + await`. Each one starts a coroutine in `viewModelScope` that waits 300 ms and then throws. The scope has a `SupervisorJob`.

## Symptom

`launch`: the app closes to the launcher about 0.1 s after the throw.
`async, no await`: nothing visible happens. The card reads `async finished, no error shown`.
`async + await`: the app stays open and the card reads `caught: sync failed for Nishant`.
Real run, API 36 emulator.

## Trace

Tag `logcatdaily` plus `AndroidRuntime`, `launch` button:

```
D logcatdaily: launch start
D logcatdaily: launch throw
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: java.lang.IllegalStateException: sync failed for Nishant
E AndroidRuntime: 	at dev.logcatdaily.samples.launchasync.LaunchAsyncViewModel$runLaunch$1.invokeSuspend(LaunchAsyncViewModel.kt:22)
```

Throw to crash: 75 ms. `async, no await`, same throw:

```
D logcatdaily: async start
D logcatdaily: async throw
D logcatdaily: async no await: no crash, no error shown
```

No `AndroidRuntime` line. The error sits in the `Deferred`. `async + await`:

```
D logcatdaily: async start
D logcatdaily: async throw
D logcatdaily: await caught java.lang.IllegalStateException: sync failed for Nishant
```

## Culprit

[`LaunchAsyncViewModel.kt:19`](LaunchAsyncViewModel.kt#L19)

```kotlin
viewModelScope.launch {
```

`launch` has no result to hand the exception to, so it goes to the thread's uncaught exception handler and the app dies. A `SupervisorJob` only stops the failure from cancelling siblings. It does not catch anything.

The quiet one is [`LaunchAsyncViewModel.kt:29`](LaunchAsyncViewModel.kt#L29): `async` stores the exception in the `Deferred`, and if `await()` is never called it is never seen.

## Fix

[`LaunchAsyncViewModel.kt:51`](LaunchAsyncViewModel.kt#L51)

```kotlin
deferred.await()
```

Wrap the `await()` in try/catch (lines 50 to 56), or pass a `CoroutineExceptionHandler` to `launch` (it must be on the root coroutine).

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample launch-async
adb logcat -s logcatdaily AndroidRuntime:E
```
