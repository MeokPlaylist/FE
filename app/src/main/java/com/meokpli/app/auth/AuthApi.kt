package com.meokpli.app.auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("login")
    suspend fun login(@Body req: LoginRequest): TokenResponse

    @POST("signUp")
    suspend fun register(@Body request: RegisterRequest): TokenResponse

    @POST("emailInspect")
    suspend fun checkEmail(@Body request: EmailInspectRequest): EmailCheckResponse

    @POST("socialLogin/google")
    suspend fun googleLogin(@Body req: GoogleLoginRequest): TokenResponse

    @POST("socialLogin/kakao")
    suspend fun kakaoLogin(@Body req: KakaoLoginRequest): TokenResponse
    @POST("reissue")
    suspend fun refresh(@Body req: RefreshRequest): TokenResponse

    /*
    //액세스 토큰 갱신
    @POST("refresh")
    suspend fun refresh(@Body req: RefreshRequest): TokenResponse
*/
    @GET("api/profile/status")
    suspend fun getProfileStatus(): ProfileStatusResponse

    @GET("api/category/status")
    suspend fun getCategoryStatus(): CategoryStatusResponse

    @GET("nicknameDuplicateCheck")
    suspend fun checkNickname(
        @Query("nickname") nickname: String
    ): NicknameCheckResponse

}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val name: String, val birthDay: String? = null)
data class GoogleLoginRequest(val idToken: String)
data class KakaoLoginRequest(val accessToken: String, val refreshToken: String)


data class EmailInspectRequest(val email: String)


data class EmailCheckResponse(val isAvailable: Boolean)
//액세스토큰 DTO
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
data class RefreshRequest(val refreshToken: String)

data class ProfileStatusResponse(val isCompleted: Boolean)
data class CategoryStatusResponse(val isCompleted: Boolean)
data class NicknameCheckResponse(val isAvailable: Boolean)
