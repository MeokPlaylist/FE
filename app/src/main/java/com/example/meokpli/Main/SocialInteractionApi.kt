package com.example.meokpli.Main

import retrofit2.http.POST
import retrofit2.http.Query

interface SocialInteractionApi {
    @POST("follow")
    suspend fun follow(@Query("nickname") nickname: String)

    // ⚠️ 경로 대소문자 BE와 동일하게: unFollow
    @POST("unFollow")
    suspend fun unFollow(@Query("nickname") nickname: String)
}