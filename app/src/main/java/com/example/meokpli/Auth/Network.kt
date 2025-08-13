package com.example.meokpli.Auth

import android.content.Context
import okhttp3.OkHttpClient
import com.example.meokpli.User.UserApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Network {
    private const val AUTH_BASE_URL = "https://meokplaylist.store/auth/"
    private const val USER_BASE_URL = "https://meokplaylist.store/user/"

    private fun okHttp(context: Context): OkHttpClient {
        val tokenManager = TokenManager(context)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun authApi(context: Context): AuthApi {
        val client = okHttp(context)
        return retrofit(AUTH_BASE_URL, client).create(AuthApi::class.java)
    }

    fun userApi(context: Context): UserApi {
        val client = okHttp(context)
        return retrofit(USER_BASE_URL, client).create(UserApi::class.java)
    }
    fun categoryApi(context: Context): com.example.meokpli.User.category.CategoryApi {
        val tokenManager = TokenManager(context)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenRefreshAuthenticator(tokenManager, AUTH_BASE_URL))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL) // "https://meokplaylist.store/auth/" 그대로 사용
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.meokpli.User.category.CategoryApi::class.java)
    }
}
/*
4) Network (공용 Retrofit/OkHttp 팩토리)
무엇을: Interceptor + Authenticator가 장착된 하나의 클라이언트 생성.
왜 이렇게:

모든 API 인스턴스가 같은 정책/타임아웃/리트라이를 공유 → 버그 줄고 유지보수 쉬움.

“리프레시 자동화”가 앱 전역에서 일관되게 동작.
 */