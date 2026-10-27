# flow-vs-shared

A small screen that runs one case at a time and prints what a collector actually receives from a `StateFlow` and from a `MutableSharedFlow` with replay 0. Four cases, no bug to fix: this one shows how the two types differ.

## Symptom

Same events, different results. A collector that starts late gets the last value from a StateFlow, but nothing from a SharedFlow. Equal values are skipped by a StateFlow and delivered by a SharedFlow.

## Trace

From a real run (API 36 emulator), `adb logcat -s logcatdaily -v threadtime`, tag and message only.

Late collector, StateFlow (`state-late`):

```
state emit Nishant online
collector starts
collector got Nishant online
state emit Nishant typing
collector got Nishant typing
```

Late collector, SharedFlow replay 0 (`shared-late`):

```
shared emit Nishant signed in
collector starts
shared emit Nishant signed out
collector got Nishant signed out
```

"Nishant signed in" never reached the collector.

Equal values, StateFlow (`state-equal`):

```
collector got Nishant
state emit Nishant
state emit Nishant
state emit Nishant online
collector got Nishant online
```

Equal values, SharedFlow (`shared-equal`):

```
shared emit Nishant
collector got Nishant
shared emit Nishant
collector got Nishant
shared emit Nishant online
collector got Nishant online
```

## Culprit

[`FlowVsSharedCases.kt:43`](FlowVsSharedCases.kt#L43)

```kotlin
val events = MutableSharedFlow<String>()
```

Replay defaults to 0, so it keeps nothing for a collector that is not there yet. Sending a one-shot event this way loses it if the screen is not collecting at that moment.

## Fix

[`FlowVsSharedCases.kt:29`](FlowVsSharedCases.kt#L29)

```kotlin
val state = MutableStateFlow("Nishant offline")
```

A StateFlow always holds a value and hands the latest one to every new collector. That suits state. It also drops a set that equals the current value ([line 57](FlowVsSharedCases.kt#L57)), so it is wrong for events that can repeat. In this sample I would keep SharedFlow for events and add `replay = 1` only if a late collector must see the last one.

## Run it

`variant` is the case: `state-late`, `shared-late`, `state-equal` or `shared-equal`.

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample flow-vs-shared --es variant state-late
adb logcat -s logcatdaily
```
