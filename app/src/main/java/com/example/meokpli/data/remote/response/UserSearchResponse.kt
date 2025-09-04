// com.example.meokpli.User.UserSearchResponse.kt
package com.example.meokpli.User

data class UserSearchResponse(
    val content: List<UserSearchDto>,
    val last: Boolean
)
data class UserSearchDto(
    val nickname: String,
    val introduction: String?
)