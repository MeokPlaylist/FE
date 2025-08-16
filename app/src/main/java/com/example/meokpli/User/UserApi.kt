package com.example.meokpli.User

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UserApi {

    @POST("setupDetailInfo")
    suspend fun saveDetail(
        @Body body: UserDetailRequest
    ): BaseResponse

    @Multipart
    @POST("setupProfile")
    suspend fun savePhoto(
        @Part profileImg: MultipartBody.Part
    ): BaseResponse

    @POST("find")
    suspend fun findUser(@Body request: FindUserRequest): FindUserResponse

    @POST("renewalPassword")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)

    @POST("newBCheck")
    suspend fun newBCheck(): retrofit2.Response<Void>

    @POST("consentAgree")
    suspend fun consentAgree(@Body request: ConsentAgreeRequest): BaseResponse


}

data class FindUserRequest(val name: String, val email: String)
data class FindUserResponse(val userId: Long)
data class ResetPasswordRequest(val userId: Long,val newPassword: String)
data class UserDetailRequest(val nickname: String, val introduction: String)
data class BaseResponse(val success: Boolean, val message: String?)


data class ConsentAgreeRequest(
    val agreed: Boolean
)