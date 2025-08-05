package com.example.meokpli.Auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("signUp")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("emailInspect")
    suspend fun checkEmail(@Body request: emailInspectRequest): EmailCheckResponse

    @POST("socialLogin")
    suspend fun oauthLogin(@Body req: OAuthRequest): LoginResponse

    @GET("api/profile/status")
    suspend fun getProfileStatus(): ProfileStatusResponse

    @GET("api/category/status")
    suspend fun getCategoryStatus(): CategoryStatusResponse


}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val name: String, val birthDay: String? = null)
data class OAuthRequest(val provider: String, val token: String)
data class LoginResponse(val jwt: String)
data class findUserRequest(val name: String, val email: String)
data class emailInspectRequest(val email: String)
data class FindUserResponse(val userId: Long)

data class EmailCheckResponse(val isAvailable: Boolean)
data class ResetPasswordRequest(val userId: Long,val newPassword: String)

data class ProfileStatusResponse(val isCompleted: Boolean)
data class CategoryStatusResponse(val isCompleted: Boolean)
