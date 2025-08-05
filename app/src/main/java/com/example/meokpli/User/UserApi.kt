package com.example.meokpli.User

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ProfileApi {

    @POST("/api/profile")
    suspend fun saveProfile(@Body req: ProfileRequest): Response<Unit>


}
data class ProfileRequest(
    val nickname: String,
    val introduction: String
)