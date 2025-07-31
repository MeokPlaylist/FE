package com.example.meokpli

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface ForgotPasswordApi {
    @POST("/renewalPassword")
    suspend fun findPassword(@Body req: ForgotPasswordRequest): BasicResponse
}

data class ForgotPasswordRequest(
    val email: String,
    val name: String,
    val birthDay: String? = null
)

data class BasicResponse(val success: Boolean)
