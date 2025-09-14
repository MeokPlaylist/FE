package com.meokpli.app.main.Interaction

import com.meokpli.app.main.SlicedResponse
import retrofit2.http.GET
import retrofit2.http.Query

// ── BE 응답 1건(항목) DTO ────────────────────────────────────────────────
// BE: GetFollowResponse { nickname, profileImgKey, introduction }
data class GetFollowResponseDto(
    val nickname: String,
    val profileImgKey: String?,
    val introduction: String?
)

// ── API 인터페이스 ─────────────────────────────────────────────────────
interface FollowApi {
    // /user/getFollowerList?page=0&size=10&sort=id,DESC
    @GET("getFollowerList")
    suspend fun getFollowerList(
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): SlicedResponse<GetFollowResponseDto>

    // /user/getFollowingList?page=0&size=10&sort=id,DESC
    @GET("getFollowingList")
    suspend fun getFollowingList(
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): SlicedResponse<GetFollowResponseDto>
    //남의 계정 팔로우 리스트 엔드포인트 만들어야함
    @GET("getOtherUserFollowerList")
    suspend fun getFollowerListOf(
        @Query("nickname") nickname: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): SlicedResponse<GetFollowResponseDto>

    @GET("getOtherUserFollowingList")
    suspend fun getFollowingListOf(
        @Query("nickname") nickname: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "id,DESC"
    ): SlicedResponse<GetFollowResponseDto>


}
