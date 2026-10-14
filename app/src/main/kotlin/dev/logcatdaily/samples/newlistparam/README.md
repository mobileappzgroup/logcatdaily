# new-list-param

A task screen with an "add tap" button, a "taps: n" counter and a list of the
open tasks (30 tasks, 20 of them not done, 14 visible). The button has nothing to do with
the list, but the list keeps recomposing.

## Symptom

I tapped the button 20 times, about every 0.4 s. The task list on screen never
changed, 14 rows with the same text. In Logcat `TaskList` ran 21 times.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: Screen recompose #1
logcatdaily: TaskList recompose #1
logcatdaily: TaskRow 1 recompose #1
...
logcatdaily: TaskRow 20 recompose #1
logcatdaily: Screen recompose #2
logcatdaily: TaskList recompose #2
...
logcatdaily: Screen recompose #21
logcatdaily: TaskList recompose #21
```

`#1` is the first composition, so 20 taps give 21. The first tap is at 5.768 s
and `#21` is at 15.161 s. The 14 rows run once and then skip. Same steps on
the fixed variant:

```
logcatdaily: Screen recompose #1
logcatdaily: TaskList recompose #1
logcatdaily: TaskRow 1 recompose #1
...
logcatdaily: TaskRow 20 recompose #1
logcatdaily: Screen recompose #2
...
logcatdaily: Screen recompose #21
```

`Screen` still runs 21 times, because it reads `taps`. `TaskList` stays at 1.

## Culprit

[`NewListParamSample.kt:56`](NewListParamSample.kt#L56)

```kotlin
tasks.filter { !it.done }
```

This runs in the screen body, so every recomposition builds a new `List`.
`TaskList(tasks: List<Task>)` takes it as a parameter. `List` is not a stable
type, and with strong skipping (on by default since Kotlin 2.0.20) an unstable
parameter is compared by instance. Same contents, new instance, so it counts
as changed and `TaskList` runs again. The rows skip because `Task` is a data
class of stable fields, compared with `equals`.

## Fix

[`NewListParamSample.kt:58`](NewListParamSample.kt#L58)

```kotlin
remember(tasks) { tasks.filter { !it.done } }
```

`remember(tasks)` keeps the same list instance until `tasks` itself changes,
and `tasks` is remembered above it ([line 31](NewListParamSample.kt#L31)). So
the parameter is the same instance on every tap and `TaskList` skips. This
only works as a skip because strong skipping is on. Without it, a composable
with a `List` parameter is not skippable at all.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample new-list-param --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample new-list-param --es variant fixed
adb logcat -s logcatdaily
```

The taps: `adb shell input tap 540 254`, 20 times, 0.4 s apart. The
coordinates hit the button on a 1080x2400 emulator.

## Video

- Long: https://youtu.be/_II773SUM5M?t=92 (Child recomposes, same list, 1:32)
