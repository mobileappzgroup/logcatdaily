# gson-standalone

A plain JVM run with Retrofit and Gson against a local server. The server answers `GET /users` with a JSON array. The interface asks for one `User`, so Gson throws "Expected BEGIN_OBJECT but was BEGIN_ARRAY". No emulator needed.

Versions: Retrofit 3.0.0, converter-gson 3.0.0, Gson 2.15.0, Kotlin 2.4.21, coroutines 1.10.2, Gradle 9.6.0, JDK 21.

## Symptom

The server sends this body ([`MockServer.kt:6`](src/main/kotlin/dev/logcatdaily/samples/gson/MockServer.kt#L6)):

```
[{"id":1,"name":"Nishant"},{"id":2,"name":"Logcat"}]
```

The broken run has no try/catch around the call, so the exception ends the program, the same way an uncaught one ends an app.

## Trace

From a real run, `./gradlew -q run --args=broken`, stack frames cut:

```
Exception in thread "main" com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was BEGIN_ARRAY at line 1 column 2 path $
See https://github.com/google/gson/blob/main/Troubleshooting.md#unexpected-json-structure
...
Caused by: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was BEGIN_ARRAY at line 1 column 2 path $
```

`BEGIN_OBJECT` is Gson's name for `{`, `BEGIN_ARRAY` is `[`. Column 2 is right after the first `[`, and `path $` is the root of the JSON. Same server, fixed variant:

```
parsed: [User(id=1, name=Nishant), User(id=2, name=Logcat)]
```

## Culprit

[`UsersApi.kt:8`](src/main/kotlin/dev/logcatdaily/samples/gson/UsersApi.kt#L8)

```kotlin
suspend fun users(): User
```

Retrofit hands Gson the return type. `User` is a class, so Gson expects the body to start with `{`. It starts with `[`.

## Fix

[`UsersApi.kt:14`](src/main/kotlin/dev/logcatdaily/samples/gson/UsersApi.kt#L14)

```kotlin
suspend fun users(): List<User>
```

The type now matches the array. Retrofit keeps the generic type from the interface, so no TypeToken is needed here. If the API sends an object that wraps the list, like `{"data":[...]}`, you model that object instead, and declaring `List<User>` gives the mirror error, "Expected BEGIN_ARRAY but was BEGIN_OBJECT". I did not run that variant.

Gson is in maintenance mode (its README), not deprecated. Moshi and kotlinx.serialization throw their own exception types.

## Run it

```
./gradlew -q run --args=broken
./gradlew -q run --args=fixed
```

## Video

- Short: 
- Reel: 
