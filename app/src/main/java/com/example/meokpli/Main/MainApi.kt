package com.example.meokpli.Main

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface MainApi {

    //가정 확정 아님
    @POST("/api/category")
    suspend fun sendCategory(@Body req: CategoryRequest): Response<Unit>


}
data class CategoryRequest(
    val mood: List<String>,
    val food: List<String>,
    val companion: List<String>,
    val regions: List<RegionReq>   // ← 추가
)
data class RegionReq(
    val sido: String,              // 예: "서울"
    val sigungu: String? = null    // 예: "구로구" (없으면 null)
)