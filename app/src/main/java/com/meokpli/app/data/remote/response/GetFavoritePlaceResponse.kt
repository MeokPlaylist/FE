package com.meokpli.app.data.remote.response;

import com.kakao.vectormap.Coordinate

data class GetFavoritePlaceResponse (
        val placeCoordinates: List<Coordinate>
)