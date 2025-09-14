package com.meokpli.app.data.remote.response

data class SearchPlaceResponse(
    val id: String,
    val place_name: String,
    val phone: String,
    val address_name: String,
    val road_address_name: String,
    val place_url: String,
    val category_group_code: String,
    val category_group_name: String
)