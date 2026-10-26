# fragment-binding

An inbox Fragment with three message rows. It starts a 2 second load (a `delay(2000)` standing in for a network call) and shows "3 new messages" when it finishes. Tapping a row opens the message Fragment on top, with `addToBackStack`.

## Symptom

Open the inbox, tap a message while it still says Loading. The Inbox Fragment stays on the back stack but its view is destroyed. About a second later the app crashes.

## Trace

From a real run (API 36 emulator), `adb logcat -s logcatdaily` plus the crash, broken variant:

```
D logcatdaily: inbox view created
D logcatdaily: inbox view destroyed, binding = null
D logcatdaily: load done, setting status
E AndroidRuntime: java.lang.NullPointerException
E AndroidRuntime: 	at dev.logcatdaily.samples.fragmentbinding.InboxFragment.getBinding(InboxFragment.kt:20)
E AndroidRuntime: 	at dev.logcatdaily.samples.fragmentbinding.InboxFragment$onViewCreated$1.invokeSuspend(InboxFragment.kt:40)
...
```

The view was destroyed 1.2 s after it was created, the load finished 2.0 s after. Same steps on the fixed variant:

```
D logcatdaily: inbox view created
D logcatdaily: load cancelled with the view
D logcatdaily: inbox view destroyed, binding = null
```

## Culprit

[`InboxFragment.kt:30`](InboxFragment.kt#L30)

```kotlin
if (broken) lifecycleScope
```

`lifecycleScope` follows the Fragment, not its view. The Fragment is still alive on the back stack, so the load keeps running, and [line 40](InboxFragment.kt#L40) reads `binding` after `onDestroyView` set it to null.

## Fix

[`InboxFragment.kt:31`](InboxFragment.kt#L31)

```kotlin
else viewLifecycleOwner.lifecycleScope
```

That scope is cancelled when the view is destroyed, so the load never reaches `binding`. I only tried one load and one tap, not Flow collectors.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.fragmentbinding.FragmentBindingActivity --es variant broken
adb shell am start -n dev.logcatdaily.samples/.fragmentbinding.FragmentBindingActivity --es variant fixed
adb logcat -s logcatdaily
```

## Video

Links follow when the post is live.
