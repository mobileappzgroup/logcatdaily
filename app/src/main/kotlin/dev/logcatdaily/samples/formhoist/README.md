# form-hoist

A Profile card with Name and Email fields and a Clear button. In the broken
version Clear does nothing to the text.

## Symptom

I typed "Asha" into Name and "asha@mail.com" into Email, then tapped Clear at
7.1 s into the recording. Both fields still showed their text at 8.3 s. Clear
did run, it just had nothing to clear.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: clear tapped
```

One line in the whole 12.8 s recording, at 7.131 s. The form never sees the
text, so it has nothing else to print. Same steps on the fixed variant, which
logs the form state on every recomposition:

```
logcatdaily: form state name='' email=''
logcatdaily: form state name='Asha' email=''
logcatdaily: form state name='Asha' email='a'
...
logcatdaily: form state name='Asha' email='asha@mail.com'
logcatdaily: form state name='' email=''
```

8 lines in 7.1 s. The last line is the Clear tap (7.114 s): both strings are
empty and both fields are empty on screen.

## Culprit

[`FormHoistSample.kt:65`](FormHoistSample.kt#L65)

```kotlin
var text by rememberSaveable {
    mutableStateOf("")
}
```

This sits inside `BrokenField`, so each field owns its own text. `BrokenForm`
only has the two field calls and a Clear button, and the button's click
handler has no variable to write to. Both versions use `rememberSaveable`, so
this has nothing to do with rotation.

## Fix

[`FormHoistSample.kt:80`](FormHoistSample.kt#L80) and
[`FormHoistSample.kt:97`](FormHoistSample.kt#L97)

```kotlin
var name by rememberSaveable {
    mutableStateOf("")
}
...
ClearButton(onClick = {
    name = ""
    email = ""
})
```

The form owns `name` and `email`. Each field takes `value` and `onValueChange`
([line 106](FormHoistSample.kt#L106)), so state goes down and events go up.
Clear is now two assignments. Hoisting does not mean a ViewModel: the state
goes to the lowest common ancestor of every composable that reads or writes
it, here the form. If nothing
outside the field needs the text, keeping it inside the field is fine.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample form-hoist --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample form-hoist --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Short: https://youtube.com/shorts/aKpqjT1L44s
- Reel: https://www.instagram.com/reel/Deb-UiJAMg3/
