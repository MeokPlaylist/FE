package com.example.meokpli.Main.Interaction

import retrofit2.http.GET
import retrofit2.http.Query

// ── BE 응답 1건(항목) DTO ────────────────────────────────────────────────
// BE: GetFollowResponse { nickname, profileImgKey, introduction }
data class GetFollowResponseDto(
    val nickname: String,
    val profileImgKey: String?,
    val introduction: String?
)

// ── Spring Data Page 래퍼 ───────────────────────────────────────────────
data class PageResponse<T>(
    val content: List<T>,
    val number: Int,
    val size: Int,
    val last: Boolean,
    val totalElements: Long? = null,
    val totalPages: Int? = null
)

// ── API 인터페이스 ─────────────────────────────────────────────────────
interface FollowApi {
    // /user/getFollowerList?page=0&size=10&sort=id,DESC
    @GET("getFollowerList")
    suspend fun getFollowerList(
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): PageResponse<GetFollowResponseDto>

    // /user/getFollowingList?page=0&size=10&sort=id,DESC
    @GET("getFollowingList")
    suspend fun getFollowingList(
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): PageResponse<GetFollowResponseDto>
    //남의 계정 팔로우 리스트 엔드포인트 만들어야함
    @GET("getFollowerListOf")
    suspend fun getFollowerListOf(
        @Query("nickname") nickname: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): PageResponse<GetFollowResponseDto>

    @GET("getFollowingListOf")
    suspend fun getFollowingListOf(
        @Query("nickname") nickname: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): PageResponse<GetFollowResponseDto>


}
