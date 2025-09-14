package com.meokpli.app.main.Favorite

import com.meokpli.app.data.remote.request.SearchPlaceRequest
import com.meokpli.app.data.remote.response.SearchPlaceResponse
import retrofit2.http.Body
import retrofit2.http.POST
interface PlaceApi {
    @POST("search")
    suspend fun searchPlace(
        @Body body: SearchPlaceRequest
    ): SearchPlaceResponse
}