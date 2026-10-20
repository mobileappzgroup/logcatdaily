# nav-dup

A Navigation Compose app with an inbox screen and a message screen. One button opens the message.

## Symptom

Tap Open message twice, fast. The message screen opens, and one back press shows the same message screen again. It takes two back presses to get to the inbox.

## Trace

From a real run (API 37 emulator), `adb logcat -s logcatdaily`, two taps 99 ms apart, then back twice:

```
D logcatdaily: stack: list
D logcatdaily: navigate detail/7
D logcatdaily: stack: list > detail/7
D logcatdaily: navigate detail/7
D logcatdaily: stack: list > detail/7 > detail/7
D logcatdaily: stack: list > detail/7
D logcatdaily: stack: list
```

After the first back the stack is still `list > detail/7`: the message screen shows again.

## Culprit

[`NavDupSample.kt:51`](NavDupSample.kt#L51)

```kotlin
nav.navigate("detail/7")
```

Every tap pushes a new entry. The button is still on screen while the first transition runs, so the second tap pushes a second `detail/7` on top of the first.

## Fix

[`NavDupSample.kt:55`](NavDupSample.kt#L55)

```kotlin
nav.navigate("detail/7") { launchSingleTop = true }
```

If the top of the back stack is already that destination, it is reused and nothing is pushed. It does not help when two taps open two different destinations.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample nav-dup --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample nav-dup --es variant fixed
adb logcat -s logcatdaily
```
The stack log reads currentBackStack, a library-internal API, with @SuppressLint. Demo only: do not copy it into an app.
