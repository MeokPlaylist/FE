package com.meokpli.app.main.Roadmap

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RoadmapApi {
    @GET("/api/roadmap")
    suspend fun getRoadmap(@Query("feedId") feedId: Long): CallInRoadMapResponse

    @GET("/place/pullOutKakao")
    suspend fun pullOutKakao(@Query("feedId") feedId: Long): PullOutKakaoPlaceResponse

    @POST("/place/saveRoadMap")
    suspend fun saveRoadMap(@Body body: SaveRoadMapPlaceRequest): retrofit2.Response<Unit>
}

data class CallInRoadMapResponse(
    val callInRoadMapDtoList: List<CallInRoadMapDto>
)

data class CallInRoadMapDto(
    val name: String,
    val addressName: String?,
    val roadAddressName: String?,
    val phone: String?,
    val kakaoCategoryName: String?,
    val photoImgUrl: String?
)
data class SaveRoadMapPlaceRequest(
    val feedId: Long,
    val saveRoadMapPlaceInfor: Map<Int, KakaoDocument>
)

data class PullOutKakaoPlaceResponse(
    val kakaoPlaceInfor: Map<Int, List<KakaoDocument>>
)
data class KakaoDocument(
    val id: String,
    val placeName: String,
    val addressName: String?,
    val roadAddressName: String?,
    val placeUrl: String?,
    val phone: String?,
    val categoryGroupCode: String?,
    val categoryGroupName: String?
)