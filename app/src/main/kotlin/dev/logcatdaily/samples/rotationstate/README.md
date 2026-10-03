# rotation-state

A cart row with a quantity stepper. Tap `+` a few times, rotate the phone,
and the broken version is back at 0.

## Symptom

I tapped `+` five times, so the quantity read 5, then rotated the emulator.
The broken version came back at 0. The fixed version came back at 5.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
composing with count=0
composing with count=1
composing with count=2
composing with count=3
composing with count=4
composing with count=5
composing with count=0
composing with count=0
```

The five taps land between 2.1 s and 3.8 s, the rotation at 4.3 s, and the
first composition after the rotation already reads 0. Same steps on the
fixed variant:

```
composing with count=0
composing with count=1
composing with count=2
composing with count=3
composing with count=4
composing with count=5
composing with count=5
composing with count=5
```

## Culprit

[`RotationStateSample.kt:41`](RotationStateSample.kt#L41)

```kotlin
remember { mutableStateOf(0) }
```

`remember` keeps the value across recompositions only. Rotation is a
configuration change, and by default it recreates the activity, so the
composition starts over and `remember` runs its initializer again: 0.

## Fix

[`RotationStateSample.kt:43`](RotationStateSample.kt#L43)

```kotlin
rememberSaveable { mutableStateOf(0) }
```

`rememberSaveable` writes the value into the saved instance state Bundle and
reads it back when the activity comes back, so it also survives process
death. An Int just works. A custom type needs a `Saver` or `@Parcelize`.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample rotation-state --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample rotation-state --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Short: https://youtube.com/shorts/OH7UdCl9-mo
- Reel: https://www.instagram.com/logcatdaily/reel/DeCOdlMDN4q/
