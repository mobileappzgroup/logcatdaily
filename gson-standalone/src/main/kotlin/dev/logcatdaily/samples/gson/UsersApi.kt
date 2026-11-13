package dev.logcatdaily.samples.gson

import retrofit2.http.GET

// Broken: the endpoint sends a JSON array, the return type is one object.
interface UsersApi {
    @GET("users")
    suspend fun users(): User
}

// Fixed: the return type matches the array.
interface UsersApiFixed {
    @GET("users")
    suspend fun users(): List<User>
}
