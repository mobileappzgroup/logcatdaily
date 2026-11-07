# agpbuiltin-standalone

A one-module app with one Kotlin class. The broken build file still applies the `org.jetbrains.kotlin.android` plugin, and AGP 9 stops the build because it already compiles Kotlin itself.

Versions: Gradle 9.6.0, AGP 9.5.1, Kotlin plugin 2.4.21 (declared in the version catalog, used only by the broken file).

## Symptom

`./gradlew :app:assembleDebug` fails while Gradle applies the plugins, before any task runs. Android Studio shows the same text on sync.

## Trace

From a real run, `app/build.gradle.kts.broken` as the build file (stack trace and Gradle's tips left out):

```
An exception occurred applying plugin request [id: 'org.jetbrains.kotlin.android', version: '2.4.21']
> Failed to apply plugin 'org.jetbrains.kotlin.android'.
   > Failed to apply plugin 'org.jetbrains.kotlin.android'
     The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
     Solution: Remove the 'org.jetbrains.kotlin.android' plugin from this project's build file: app/build.gradle.kts.
...
BUILD FAILED in 24s
```

Same file with the plugin line removed (`app/build.gradle.kts.fixed`):

```
> Task :app:compileDebugKotlin
...
BUILD SUCCESSFUL in 18s
34 actionable tasks: 34 executed
```

Both runs used `--no-daemon`. The Kotlin file compiles in the fixed run without the plugin.

I did not get "Cannot add extension with name 'kotlin'" here. With this setup the message above comes first. A second run with Kotlin plugin 2.2.20 printed the same sentence with a different link.

Wrapper note: with Gradle 8.14.3 and AGP 9.5.1 the build stops earlier with `Minimum supported Gradle version is 9.6.0. Current version is 8.14.3.`

## Culprit

[`app/build.gradle.kts.broken:3`](app/build.gradle.kts.broken#L3)

```kotlin
alias(libs.plugins.kotlin.android)
```

AGP 9 has Kotlin support built in, so a second Kotlin Android plugin on the same module is rejected.

## Fix

[`app/build.gradle.kts.fixed:1-3`](app/build.gradle.kts.fixed#L1-L3)

```kotlin
plugins {
    alias(libs.plugins.android.application)
}
```

Delete the line. Kotlin sources still compile, because AGP now does that itself.

## Run it

```
cd agpbuiltin-standalone
./gradlew :app:assembleDebug
```

`app/build.gradle.kts` is the fixed file. To see the failure, copy `app/build.gradle.kts.broken` over it.
