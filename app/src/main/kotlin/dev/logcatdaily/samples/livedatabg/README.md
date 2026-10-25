# livedata-bg

A screen with a Load button and a list. Tapping Load fetches three names in a coroutine on `Dispatchers.IO` and puts them in a `MutableLiveData`.

## Symptom

Tap Load once. The app closes straight to the launcher, 0.42 s after the tap. The list never shows.

## Trace

From a real run (API 37 emulator), `adb logcat`, one tap on Load, broken variant:

```
D logcatdaily: items shown: 0 variant=broken
D logcatdaily: load: tapped variant=broken
E AndroidRuntime: FATAL EXCEPTION: DefaultDispatcher-worker-1
E AndroidRuntime: java.lang.IllegalStateException: Cannot invoke setValue on a background thread
E AndroidRuntime: 	at androidx.lifecycle.LiveData.assertMainThread(LiveData.java:501)
E AndroidRuntime: 	at androidx.lifecycle.LiveData.setValue(LiveData.java:307)
E AndroidRuntime: 	at androidx.lifecycle.MutableLiveData.setValue(MutableLiveData.java:50)
```

Tap to crash: 0.42 s. Same steps on the fixed variant:

```
D logcatdaily: items shown: 0 variant=fixed
D logcatdaily: load: tapped variant=fixed
D logcatdaily: items shown: 3 variant=fixed
```

## Culprit

[`LiveDataBgSample.kt:46`](LiveDataBgSample.kt#L46)

```kotlin
_items.value = result
```

The coroutine on line 44 runs on `Dispatchers.IO`, so this line runs on a worker thread. `setValue` checks the thread and throws `IllegalStateException` when it is not the main thread. Loading on IO is fine. Setting the value from there is not.

## Fix

[`LiveDataBgSample.kt:53`](LiveDataBgSample.kt#L53)

```kotlin
_items.postValue(result)
```

`postValue` hands the update to the main thread for you. It is asynchronous, so if you post several times quickly, observers can see only the last value.

Second fix, [`LiveDataBgSample.kt:60-62`](LiveDataBgSample.kt#L60-L62), the `fixed-main` variant:

```kotlin
withContext(Dispatchers.Main) {
    _items.value = result
}
```

Switch to the main thread first, then call `setValue`. Every value is delivered in order.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample livedata-bg --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample livedata-bg --es variant fixed
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample livedata-bg --es variant fixed-main
adb logcat
```

## Video

- Short: https://youtube.com/shorts/ZIjPCzwAbHM
- Instagram: @logcatdaily reel, 2026-10-25
