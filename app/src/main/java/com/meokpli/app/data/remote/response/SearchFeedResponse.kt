package com.meokpli.app.data.remote.response

data class SearchFeedResponse(
    val urlsMappedByFeedIds: List<Map<Long, String>>
)
