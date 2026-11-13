package dev.logcatdaily.samples.gson

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Run with: ./gradlew -q run --args=broken   or   --args=fixed
// broken: no try/catch, so the exception ends the program like an uncaught crash.
fun main(args: Array<String>) = runBlocking {
    val variant = args.firstOrNull() ?: "broken"
    val server = startMockServer()
    val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:${server.address.port}/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    println("server sends: $USERS_BODY")
    try {
        if (variant == "fixed") {
            val users = retrofit.create(UsersApiFixed::class.java).users()
            println("parsed: $users")
        } else {
            val user = retrofit.create(UsersApi::class.java).users()
            println("parsed: $user")
        }
    } finally {
        server.stop(0)
    }
}
