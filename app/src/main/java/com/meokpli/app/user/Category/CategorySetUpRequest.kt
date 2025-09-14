package com.meokpli.app.user.Category

data class CategorySetUpRequest(
    val categories: List<String>,
    val regions: List<String> = emptyList() // = "지역" (선택 안했으면 빈배열)
)