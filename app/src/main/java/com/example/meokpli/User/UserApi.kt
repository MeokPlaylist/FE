package com.example.meokpli.User

import com.example.meokpli.Auth.FindUserResponse
import com.example.meokpli.Auth.ResetPasswordRequest
import com.example.meokpli.Auth.findUserRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {

    @POST("/api/profile")
    suspend fun saveProfile(@Body req: UserRequest): Response<Unit>

    @POST("find")
    suspend fun findUser(@Body request: findUserRequest): FindUserResponse

    @POST("renewalPassword")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)

}
data class UserRequest(
    val nickname: String,
    val introduction: String
)