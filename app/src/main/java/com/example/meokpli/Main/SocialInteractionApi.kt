package com.example.meokpli.Main

import android.R
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.GET
interface SocialInteractionApi {
    @POST("follow")
    suspend fun follow(@Query("nickname") nickname: String)

    // 경로 대소문자 BE와 동일하게: unFollow
    @POST("unFollow")
    suspend fun unFollow(@Query("nickname") nickname: String)


    @GET("userPageDistinction")
    suspend fun getOtherUserPage(@Query("nickname") nickname: String): UserPageResponseWrapper

    @GET("initRecommendRestaurant")
    suspend fun getInitRecommendRestaurant(): Map<String, List<String>>

    @POST("recommendRestaurant")
    suspend fun getRecommendRestaurant(
        @Body request: RecommendRestaurantRequest // ex) ["서울:강남구","서울:용산구"]
    ): Map<String, List<String>>
}

data class RecommendRestaurantRequest (val regions: List<String>)

data class UserPageResponseWrapper(
    val userPageDto: OtherUserPageResponse
)

data class OtherUserPageResponse(
    val feedNum: Long,
    val followingNum: Long,
    val followerNum: Long,
    val userNickname: String,
    val userIntro: String?,
    val profileUrl: String?,
    val feedIdsGroupedByYear: Map<Int, List<Long>>,
    val feedIdsGroupedByRegion: Map<String, List<Long>>,
    val urlMappedByFeedId: Map<Long, String>,
    val isMe: Boolean // BE가 보내주면 활용(옵션)
)