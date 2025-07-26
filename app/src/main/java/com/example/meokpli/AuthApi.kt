package com.example.meokpli

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.time.LocalDate

interface AuthApi {
    @POST("/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("/signUp")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("/emailInspect")
    suspend fun checkEmail(@Body request: emailInspectRequest): EmailCheckResponse

    @POST("/social/login")
    suspend fun oauthLogin(@Body req: OAuthRequest): LoginResponse
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val name: String, val birthDay: String)
data class OAuthRequest(val provider: String, val idToken: String)
data class LoginResponse(val jwt: String)
data class emailInspectRequest(val email: String)
data class EmailCheckResponse(val isAvailable: Boolean)