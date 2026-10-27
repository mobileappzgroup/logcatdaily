# kspmigrate-standalone

A one-module Room app that builds with KSP. The broken Gradle file still uses the old `kotlinOptions` block, which stops compiling on Kotlin 2.4.21.

Versions: Gradle 8.14.3, AGP 8.13.2, Kotlin 2.4.21, KSP 2.3.12, Room 2.8.4.

## Symptom

Sync fails in Android Studio with one error on line 25 of the app build file. The same file with the `compilerOptions` block builds.

## Trace

From a real run, `./gradlew :app:assembleDebug`, with `app/build.gradle.kts.broken` as the build file (the stack trace and Gradle's tips are left out):

```
e: .../kspmigrate-standalone/app/build.gradle.kts:25:9: Using 'jvmTarget: String' is an error. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln
...
BUILD FAILED in 1s
```

With `app/build.gradle.kts.fixed`:

```
> Task :app:assembleDebug
BUILD SUCCESSFUL in 1s
36 actionable tasks: 2 executed, 34 up-to-date
```

The fixed run was incremental, so 1s is not a clean build time. Both runs also print a warning that Gradle 8.14.3 is deprecated for the Kotlin plugin; I left it alone.

## Culprit

[`app/build.gradle.kts.broken:24-26`](app/build.gradle.kts.broken#L24-L26)

```kotlin
kotlinOptions {
    jvmTarget = "17"
}
```

`jvmTarget` as a `String` inside `kotlinOptions` is rejected by the Kotlin Gradle plugin here, with the error above pointing at the `compilerOptions` DSL.

## Fix

[`app/build.gradle.kts:28-32`](app/build.gradle.kts#L28-L32), plus the import on [line 1](app/build.gradle.kts#L1)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
...
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

The target is now a typed property on the top-level `kotlin` block, outside `android`.

## Run it

```
cd kspmigrate-standalone
./gradlew :app:assembleDebug
```

`app/build.gradle.kts` is the fixed file. To see the failure, copy `app/build.gradle.kts.broken` over it.
