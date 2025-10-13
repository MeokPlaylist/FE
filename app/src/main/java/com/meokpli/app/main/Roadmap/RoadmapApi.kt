 package com.meokpli.app.main.Roadmap

import okhttp3.Address
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

 interface RoadmapApi {
    @GET("create")
    suspend fun createRoadmap(@Query("feedId") feedId: Long): CreateRoadMapResponse

    @GET("pullOutKakao")
    suspend fun pullOutKakao(@Query("feedId") feedId: Long): PullOutKakaoPlaceResponse

    @POST("saveRoadMap")
    suspend fun saveRoadMap(@Body body: SaveRoadMapPlaceRequest): Response<Unit>
}

data class CreateRoadMapResponse(
    val roadMapId: Long,
    val roadMapCandidateDto: List<RoadMapCandidateDto>
)

 data class RoadMapCandidateDto(
     val roadMapPlaceId: Long,
     val photoUrl: String,
     val dateTime: String,
     val dayIndex: Integer,
     val orderIndex: Integer,
     val candidates: List<PlaceCandidate>
 )
 data class PlaceCandidate(
     val placeId: Long,
     val placeName: String,
     val address: String
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
    val title: String,
    val saveRoadMapPlaceInfor: Map<Long, Long>
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