package com.meokpli.app.data.remote.request

data class FeedSearchRequest(
    val categories: List<String>,
    val regions: List<String>
)
