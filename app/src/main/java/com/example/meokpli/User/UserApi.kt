package com.example.meokpli.User

import com.google.android.gms.common.api.BooleanResult
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import com.example.meokpli.data.remote.response.MyPageResponse
import com.example.meokpli.data.remote.response.PersonalInfoResponse

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

    @GET("personalInfor")
    suspend fun getPersonalInfo(): PersonalInfoResponse

    @GET("mypage")
    suspend fun  getMyPage(): myPageResponse
    //추가 필요


}

data class FindUserRequest(val name: String, val email: String)
data class FindUserResponse(val userId: Long)
data class ResetPasswordRequest(val userId: Long,val newPassword: String)
data class UserDetailRequest(val nickname: String, val introduction: String)
data class BaseResponse(val isAvailable: Boolean, val message: String?)
data class myPageResponse(
    val feedNum : Long,
    val followingNum: Long,
    val followerNum: Long,
    val userNickname: String,
    val userIntro: String,
    val profileUrl: String,
    val urlGroupedByYear: Map<Int, List<String>>,
    val urlMappedByFeedId: Map<Long, String>
)
data class ConsentAgreeRequest(
    val isAvailable: Boolean
)
data class PersonalInfoResponse(
    val name: String?,
    val email: String?,
    val birthDay: String?,    // ISO 문자열로 오면 화면에서 포맷팅
    val createdAt: String?,
    val OauthUser: Boolean// 필요 시 사용
)

data class UserPageResponse(
    val feedNum: Long,
    val followingNum: Long,
    val followerNum: Long,
    val userNickname: String,
    val userIntro: String?,
    val profileUrl: String?,
    val feedId: List<Long>?,
    val feedMainPhotoUrls: Map<Int, List<String>>?,
    val isMe: Boolean
)