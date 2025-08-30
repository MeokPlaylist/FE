package com.example.meokpli.data.remote.response

data class PersonalInfoResponse(
    val name: String?,
    val email: String?,
    val birthDay: String?,    // ISO 문자열로 오면 화면에서 포맷팅
    val createdAt: String?,
    val OauthUser: Boolean// 필요 시 사용
)