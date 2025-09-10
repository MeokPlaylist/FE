package com.example.meokpli.main.Home

import retrofit2.http.*

interface CommentApi {

    // 댓글 목록 조회
    @GET("feed/{feedId}/comments")
    suspend fun getComments(
        @Path("feedId") feedId: Long
    ): PageResponse<Comment>

    // 댓글 작성
    @POST("feed/{feedId}/comments")
    suspend fun postComment(
        @Path("feedId") feedId: Long,
        @Body req: CommentPostRequest
    ): Comment

    // 댓글 수정
    @PUT("comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Long,
        @Body req: CommentUpdateRequest
    ): Unit

    // 댓글 삭제
    @DELETE("comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Long
    ): Unit
}

/* 댓글 DTO */
data class Comment(
    val id: Long,
    val author: String,
    val content: String,
    val createdAt: String,
    val avatarUrl: String? = null
)

data class CommentPostRequest(val content: String)
data class CommentUpdateRequest(val content: String)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val last: Boolean
)