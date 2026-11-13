# duplicateclass-standalone

An empty app with two dependencies that ship the same classes: `protobuf-java` and `protobuf-javalite`. `assembleDebug` stops in `:app:checkDebugDuplicateClasses`. No emulator needed.

Versions: AGP 9.5.1, Gradle 9.6.0, protobuf 3.25.5, compileSdk 36, JDK 21.

## Symptom

`./gradlew --no-daemon :app:assembleDebug` fails after 28 seconds:

```
> Task :app:checkDebugDuplicateClasses FAILED
...
Execution failed for task ':app:checkDebugDuplicateClasses' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing com.android.build.gradle.internal.tasks.CheckDuplicatesRunnable
   > Duplicate class com.google.protobuf.AbstractMessageLite found in modules protobuf-java-3.25.5.jar -> protobuf-java-3.25.5 (com.google.protobuf:protobuf-java:3.25.5) and protobuf-javalite-3.25.5.jar -> protobuf-javalite-3.25.5 (com.google.protobuf:protobuf-javalite:3.25.5)
     Duplicate class com.google.protobuf.AbstractMessageLite$Builder found in modules ...
...
BUILD FAILED in 28s
```

357 "Duplicate class" lines in my run, one per class, all from the same two jars.

## Culprit

[`app/build.gradle.kts:29-30`](app/build.gradle.kts#L29)

```kotlin
implementation(libs.protobuf.java)
implementation(libs.protobuf.javalite)
```

"Modules" in this message are the two library jars. Both put classes with the same full names on the runtime classpath, and AGP checks for that before dexing. In a real app the second jar usually comes in through another library, not your own line.

## Fix

[`app/build.gradle.kts.fixed:23-25`](app/build.gradle.kts.fixed#L23)

```kotlin
configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}
```

Same command: `checkDebugDuplicateClasses` passes, BUILD SUCCESSFUL, and `:app:dependencies --configuration debugRuntimeClasspath` shows one protobuf jar, `protobuf-javalite:3.25.5`.

Keep the jar your code needs and check that the library which pulled the other one still works. `configureEach` excludes it from every configuration; if only one dependency brings it, exclude it on that dependency instead. If the duplicates are one library at two versions, align the versions instead of excluding.

## Run

```
./gradlew --no-daemon :app:assembleDebug
cp app/build.gradle.kts.fixed app/build.gradle.kts
./gradlew --no-daemon :app:assembleDebug
```
