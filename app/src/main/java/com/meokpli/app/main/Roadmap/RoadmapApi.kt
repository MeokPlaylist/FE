 package com.meokpli.app.main.Roadmap

import okhttp3.Address
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

 interface RoadmapApi {
    @GET("create")
    suspend fun createRoadmap(@Query("feedId") feedId: Long): List<RoadMapCandidateDto>

    @POST("save")
    suspend fun saveRoadMap(@Body body: SaveRoadMapPlaceRequest): Response<Unit>

     @GET("load")
     suspend fun loadRoadMap(@Query("feedId") feedId: Long): LoadRoadMapResponse
}
 data class LoadRoadMapResponse(
     val title: String,
     val firstDayAndTime: String?,
     val isMine: Boolean,
     val loadRoadMapPlacesList: List<LoadRoadMapPlace>
 )

 data class LoadRoadMapPlace(
     val placeId: Long,
     val name: String?,
     val address: String?,
     val phone: String?,
     val presignedGetPhotoUrl: String,
     val dayIndex: Int,
     val orderIndex: Int,
     var isFavorite: Boolean
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

 data class SaveRoadMapPlaceRequest(
     val feedId: Long,
     val title: String,
     val places: List<SaveRoadMapPlaceItem>
 )

 data class SaveRoadMapPlaceItem(
     val roadMapPlaceId: Long,
     val selectedPlaceId: Long?,     // 선택된 후보 (없으면 null)
     val customPlaceName: String?,   // 직접 입력한 이름
     val customAddress: String?      // 직접 입력한 주소
 )
