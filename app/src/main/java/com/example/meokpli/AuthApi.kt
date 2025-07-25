package com.example.meokpli

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("/auth/register")
    suspend fun register(@Body req: RegisterRequest): LoginResponse

    @POST("/auth/oauth")
    suspend fun oauthLogin(@Body req: OAuthRequest): LoginResponse
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val nickname: String, val birth: String)
data class OAuthRequest(val provider: String, val token: String)
data class LoginResponse(val jwt: String, val message: String)
