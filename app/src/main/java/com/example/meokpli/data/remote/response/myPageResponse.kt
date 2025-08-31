package com.example.meokpli.data.remote.response

data class MyPageResponse(
    val feedNum: Long,
    val followingNum: Long,
    val followerNum: Long,
    val userNickname: String,
    val userIntro: String,
    val profileUrl: String,
    val feedIdsGroupedByYear: Map<Int, List<Long>>,
    val feedIdsGroupedByRegion: Map<String, List<Long>>,
    val urlMappedByFeedId: Map<Long, String>
)
