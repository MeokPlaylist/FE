package com.example.meokpli.Main

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface MainApi {
    @POST("/api/category")
    suspend fun sendCategory(@Body req: CategoryRequest): Response<Unit>


}
data class CategoryRequest(
    val mood: List<String>,
    val food: List<String>,
    val companion: List<String>
)