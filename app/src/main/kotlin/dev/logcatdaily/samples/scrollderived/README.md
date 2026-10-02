# scroll-derived

An inbox list of 100 rows with a "scroll to top" button that should show up
once the first row is off screen.

## Symptom

Scroll the list and watch Logcat. The broken version recomposes the whole
screen every time a new row crosses the top edge: 44 recompositions in about
4.5 seconds of scrolling, for a button that only changed once (hidden to
shown).

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
recompose #1
recompose #2
recompose #3
recompose #4
...
recompose #43
recompose #44
```

44 lines between 1.5 s and 5.9 s. Same scroll on the fixed variant:

```
recompose #1
recompose #2
```

## Culprit

[`ScrollDerivedSample.kt:50`](ScrollDerivedSample.kt#L50)

```kotlin
listState.firstVisibleItemIndex > 0
```

Reading `firstVisibleItemIndex` straight in the composable body subscribes the
whole function to that snapshot state. The index changes on every row that
crosses the top, so the composable runs again on each one, even though the
boolean it feeds only flips once.

## Fix

[`ScrollDerivedSample.kt:52`](ScrollDerivedSample.kt#L52)

```kotlin
val derived by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```

`derivedStateOf` reads the index for you and only tells the composable when
the result changes. Fast-changing input, slow-changing output: that is the
case it exists for.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scroll-derived --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scroll-derived --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Short: https://youtube.com/shorts/kMD2A4Q7-U8
- Reel: https://www.instagram.com/logcatdaily/reel/Dd0H0gpI7KK/
