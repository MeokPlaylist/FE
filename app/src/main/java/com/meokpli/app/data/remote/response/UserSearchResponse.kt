package com.meokpli.app.user

data class UserSearchResponse(
    val content: List<UserSearchDto>,
    val hasNext: Boolean
)
data class UserSearchDto(
    val nickname: String,
    val introduction: String?
)