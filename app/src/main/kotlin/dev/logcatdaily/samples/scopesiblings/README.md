# scope-siblings

Two uploads run side by side. A takes 3 steps of 400 ms. B fails after 600 ms.
The first two variants differ in the scope builder (`coroutineScope` or `supervisorScope`), and supervisorScope's B gets a handler. A third variant drops that handler to show the crash.

## Symptom

With `coroutineScope`, B failing at 600 ms stops A after its first step and
the exception lands in my outer `catch`. With `supervisorScope`, B fails and A
still runs all 3 steps.

## Trace

From the recording (emulator, API 36, kotlinx-coroutines 1.9.0), tag `logcatdaily`, `coroutineScope`:

```
A step 1
B threw upload B failed
A cancelled at step 1
outer caught upload B failed
```

Four lines in about 200 ms after A's first step. Same steps with `supervisorScope`:

```
A step 1
B threw upload B failed
A step 2
A step 3
A done step 3
outer finished, nothing thrown
```

## Culprit

[`ScopeSiblingsViewModel.kt:35`](ScopeSiblingsViewModel.kt#L35)

```kotlin
coroutineScope {
```

When a child of `coroutineScope` throws, the scope cancels every other child and
rethrows to the caller. Here A gets a `CancellationException` at its next
`delay`. That is the right default when the children are one job that must
succeed together.

## Fix

[`ScopeSiblingsViewModel.kt:53`](ScopeSiblingsViewModel.kt#L53)

```kotlin
supervisorScope {
    launch { uploadA() }
    launch(uploadBHandler) {
```

Use `supervisorScope` when the children are independent. A failure no longer
cancels siblings, and it is not rethrown to the caller. The scope does not pass a
child's failure up, so B needs its own `CoroutineExceptionHandler`
([line 28](ScopeSiblingsViewModel.kt#L28)); without it the exception reaches the
thread's uncaught handler and crashes the app. A try/catch inside B works too.

## Without the handler

Third variant, `supervisornohandler`: same as `supervisorScope`, but B's `launch`
has no `CoroutineExceptionHandler` ([line 73](ScopeSiblingsViewModel.kt#L73)).
On the emulator the app crashed after only
`A step 1`:

```
D logcatdaily: A step 1
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: Process: dev.logcatdaily.samples, PID: 4816
E AndroidRuntime: java.lang.IllegalStateException: upload B failed
```

A never reached step 2, and neither `outer caught` nor `outer finished` was logged.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scope-siblings --es variant coroutinescope
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scope-siblings --es variant supervisorscope
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scope-siblings --es variant supervisornohandler
adb logcat -s logcatdaily
```

Without a variant the screen waits for a tap on either button.

Nishant Chauhan
