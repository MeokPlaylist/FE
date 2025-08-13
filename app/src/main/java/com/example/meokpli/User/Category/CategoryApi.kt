package com.example.meokpli.User.category

import com.example.meokpli.User.BaseResponse
import com.example.meokpli.User.Category.FeedCategorySetUpRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface CategoryApi {
    // 유저의 기본 카테고리 셋업 저장
    @POST("categorySet") // ✅ 백엔드 실제 경로에 맞게 조정
    suspend fun setupCategories(@Body body: CategorySetUpRequest): BaseResponse

    // 피드용 카테고리 셋업(큰주제 + 선택들)
    @POST("api/category/feed/setup") // ✅ 백엔드 실제 경로에 맞게 조정
    suspend fun setupFeedCategories(@Body body: FeedCategorySetUpRequest): BaseResponse
}