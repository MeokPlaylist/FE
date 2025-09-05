package com.example.meokpli.User

data class UserSearchResponse(
    val content: List<UserSearchDto>,
    val hasNext: Boolean
)
data class UserSearchDto(
    val nickname: String,
    val introduction: String?
)