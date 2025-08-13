package com.example.meokpli.common

import com.google.gson.Gson
import retrofit2.HttpException

data class ApiError(val code: String?, val message: String?)

object ApiErrorParser {
    private val gson = Gson()
    fun from(e: HttpException): ApiError? = try {
        val raw = e.response()?.errorBody()?.string()
        if (raw.isNullOrBlank()) null else gson.fromJson(raw, ApiError::class.java)
    } catch (_: Exception) { null }
}
