# read-too-high

A "Release notes" screen: a parallax header that moves at half the scroll
speed, then 30 rows in a `verticalScroll` column. The screen scrolls fine.
The problem is only in Logcat.

## Symptom

Nothing looks wrong on the phone. I did 8 swipes (4 down, 4 up, one per
second) and the screen composable ran again, many times, while the list scrolled.
The 30 rows did not.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: Screen recompose #1
logcatdaily: Header recompose #1
logcatdaily: Row 0 recompose #1
...
logcatdaily: Row 29 recompose #1
logcatdaily: Screen recompose #2
logcatdaily: Header recompose #2
...
logcatdaily: Screen recompose #56
logcatdaily: Header recompose #56
```

`#1` is the first composition. From `#2` at 5.648 s to `#56` at 14.254 s is 8.6 s,
so 55 recompositions of both `Screen` and `Header`. The 30 rows stay at `#1`.
The same swipes on two other saved runs gave 57 and 114 for `Screen`, so the
count follows how many frames the scroll produces, not a fixed number. Same steps on
the fixed variant:

```
logcatdaily: Screen recompose #1
logcatdaily: Header recompose #1
logcatdaily: Row 0 recompose #1
...
logcatdaily: Row 29 recompose #1
```

32 lines, all `#1`, and nothing after them. It was 1 in every run I made.

## Culprit

[`ReadTooHighSample.kt:47`](ReadTooHighSample.kt#L47)

```kotlin
Modifier.offset(y = with(density) { (scroll.value / 2).toDp() })
```

`scroll.value` is state, and this line reads it while the screen is composing.
So the whole `ReadTooHighSample` function re-runs whenever the scroll
position changes (at most once per frame). `Header` gets a different `Modifier`
each time (the offset value changed), so it runs again too. The rows only take an `Int` that never changes, so they skip.

## Fix

[`ReadTooHighSample.kt:49`](ReadTooHighSample.kt#L49)

```kotlin
Modifier.offset { IntOffset(0, scroll.value / 2) }
```

The lambda version of `offset` reads the value during layout, not during
composition. Scrolling now re-runs layout only, and the composables never run
again. `graphicsLayer { }` does the same for draw-time effects.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample read-too-high --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample read-too-high --es variant fixed
adb logcat -s logcatdaily
```

The swipes: `adb shell input swipe 540 1800 540 600 300` four times, then
`adb shell input swipe 540 600 540 1800 300` four times, 1 s apart.

## Video

- Long: https://youtu.be/_II773SUM5M?t=20 (Screen recomposes on scroll, 0:20)
