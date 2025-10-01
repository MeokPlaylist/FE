package com.meokpli.app.main.Favorite

import com.meokpli.app.data.remote.request.RemoveFavoriteRequest
import com.meokpli.app.data.remote.request.SaveFavoriteRequest
import com.meokpli.app.data.remote.request.SearchPlaceRequest
import com.meokpli.app.data.remote.response.SearchPlaceResponse
import com.meokpli.app.data.remote.response.GetFavoritePlaceResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
interface PlaceApi {
    @POST("search")
    suspend fun searchPlace(
        @Body body: SearchPlaceRequest
    ): SearchPlaceResponse

    @GET("getFavorite")
    suspend fun getFavorite(): GetFavoritePlaceResponse

    @POST("saveFavorite")
    suspend fun saveFavorite(@Body body: SaveFavoriteRequest)

    @POST("removeFavorite")
    suspend fun removeFavorite(@Body body: RemoveFavoriteRequest)
}