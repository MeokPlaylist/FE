package com.example.meokpli.User

import com.google.android.gms.common.api.BooleanResult
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UserApi {
    @POST("setupDetailInfo")
    suspend fun saveDetail(
        @Body body: UserDetailRequest
    )

    @Multipart
    @POST("setupProfile")
    suspend fun savePhoto(
        @Part profileImg: MultipartBody.Part
    )

    @POST("find")
    suspend fun findUser(@Body request: FindUserRequest): FindUserResponse

    @POST("renewalPassword")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)

    @POST("newBCheck")
    suspend fun newBCheck(): retrofit2.Response<Void>

    @POST("consentAgree")
    suspend fun consentAgree(@Body request: ConsentAgreeRequest): BaseResponse
    //profile feed 엔드포인트 바꿀거
    @GET("me")
    suspend fun getMe(): MeResponse


}

data class FindUserRequest(val name: String, val email: String)
data class FindUserResponse(val userId: Long)
data class ResetPasswordRequest(val userId: Long,val newPassword: String)
data class UserDetailRequest(val nickname: String, val introduction: String)
data class BaseResponse(val isAvailable: Boolean, val message: String?)


data class ConsentAgreeRequest(
    val isAvailable: Boolean
)

data class MeResponse(
    val nickname: String? = null,
    val introduction: String? = null,
    val postCount: Int? = null,
    val following: Int? = null,
    val followers: Int? = null,
    val profileImage: String? = null
)