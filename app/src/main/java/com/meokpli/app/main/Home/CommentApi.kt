package com.meokpli.app.main.Home

import retrofit2.http.*

interface CommentApi {

    // 댓글 조회
    @GET("getFeedComments")
    suspend fun getComments(
        @Query("feedId") feedId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): SliceResponse<GetFeedCommentDto>

    // 댓글 작성 (Form 방식)
    @FormUrlEncoded
    @POST("writeFeedComments")
    suspend fun writeComment(
        @Field("feedId") feedId: Long,
        @Field("nickname") nickname: String,
        @Field("content") content: String
    )
}

// 서버에서 내려주는 DTO
data class GetFeedCommentDto(
    val profileImgUrl: String?,
    val nickname: String,
    val duration: String,   // OffsetDateTime → 문자열
    val content: String
)

// Spring Slice 구조
data class SliceResponse<T>(
    val content: List<T>,
    val last: Boolean? = null
)

// UI 모델
data class UiComment(
    val author: String,
    val avatarUrl: String?,
    val content: String,
    val createdAt: String
)

// 변환 함수
fun GetFeedCommentDto.toUi(): UiComment {
    return UiComment(
        author = nickname,
        avatarUrl = profileImgUrl,
        content = content,
        createdAt = duration
    )
}
