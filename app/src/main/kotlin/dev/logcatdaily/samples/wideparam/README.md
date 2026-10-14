# wide-param

A "taps: n" button and a card that shows the name "Asha". The state is one
data class, `UiState(name, unread, taps)`, and the button changes `taps`.
The card only shows `name`, but it recomposes on every tap.

## Symptom

I tapped the button 20 times, about every 0.4 s. The name on the card never
changed. In Logcat `NameCard` ran 21 times, once per tap plus the first
composition.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: Screen recompose #1
logcatdaily: NameCard recompose #1
logcatdaily: Screen recompose #2
logcatdaily: NameCard recompose #2
...
logcatdaily: Screen recompose #21
logcatdaily: NameCard recompose #21
```

42 lines. The first tap is at 5.143 s and `#21` at 14.508 s, and the two
composables always log together. Same steps on the fixed variant:

```
logcatdaily: Screen recompose #1
logcatdaily: NameCard recompose #1
logcatdaily: Screen recompose #2
...
logcatdaily: Screen recompose #21
```

22 lines. `Screen` runs 21 times because it reads `taps`. `NameCard` stays at 1.

## Culprit

[`WideParamSample.kt:48`](WideParamSample.kt#L48)

```kotlin
if (broken) NameCard(state) else NameCard(state.name)
```

The broken branch is the first call, and it passes the whole `UiState` to
`NameCard(state: UiState)` ([line 54](WideParamSample.kt#L54)). Nothing here
is unstable: `UiState` is a data class of stable fields, compared with
`equals`. But every tap is `state.copy(taps = state.taps + 1)`, and a copy
with a different `taps` is not equal to the old one. So the parameter counts
as changed and the card runs again, even though it never reads `taps`.
Strong skipping does not help, because the value really is different.

## Fix

[`WideParamSample.kt:48`](WideParamSample.kt#L48), same line, the second call

```kotlin
NameCard(state.name)
```

The overload at [line 61](WideParamSample.kt#L61) takes `name: String`. A
`String` that did not change is equal, so the card skips. Pass a child only
the fields it reads. In the sample both `NameCard` overloads exist so one APK
can show both runs. In real code you would just change the signature.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample wide-param --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample wide-param --es variant fixed
adb logcat -s logcatdaily
```

The taps: `adb shell input tap 540 254`, 20 times, 0.4 s apart. The
coordinates hit the button on a 1080x2400 emulator.

## Video

- Long: https://youtu.be/_II773SUM5M?t=164 (Card recomposes, same name, 2:44)
