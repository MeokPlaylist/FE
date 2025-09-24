package com.meokpli.app.auth

import TokenManager
import android.content.Context
import com.meokpli.app.main.Favorite.PlaceApi
import com.meokpli.app.main.Home.CommentApi
import com.meokpli.app.main.Interaction.FollowApi
import com.meokpli.app.main.MainApi
import com.meokpli.app.main.Roadmap.RoadmapApi
import okhttp3.OkHttpClient
import com.meokpli.app.user.UserApi
import com.meokpli.app.user.Category.CategoryApi
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.meokpli.app.main.SocialInteractionApi

object Network {
    private const val AUTH_BASE_URL = "https://meokplaylist.shop/auth/"
    private const val USER_BASE_URL = "https://meokplaylist.shop/user/"
    private const val FEED_BASE_URL = "https://meokplaylist.shop/feed/"
    private const val SOCIAL_BASE_URL = "https://meokplaylist.shop/socialInteraction/"
    private const val PLACE_BASE_URL = "https://meokplaylist.shop/place/"
    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private fun debugClient(context: Context, withAuth: Boolean): OkHttpClient {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val tokenManager = TokenManager(context.applicationContext)

        val b = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .addInterceptor(log)

        if (withAuth) {
            val authApi = retrofit(AUTH_BASE_URL, OkHttpClient.Builder().build()).create(AuthApi::class.java)
            b.addInterceptor(AuthInterceptor(tokenManager))
            b.authenticator(TokenAuthenticator(authApi, tokenManager))
        }
        return b.build()
    }

    fun authApi(context: Context): AuthApi =
        retrofit(AUTH_BASE_URL, debugClient(context, withAuth = false)).create(AuthApi::class.java)

    fun userApi(context: Context): UserApi =
        retrofit(USER_BASE_URL, debugClient(context, withAuth = true)).create(UserApi::class.java)

    fun categoryApi(context: Context): CategoryApi =
        retrofit(USER_BASE_URL, debugClient(context, withAuth = true)).create(CategoryApi::class.java)

    fun feedApi(context: Context): MainApi =
        retrofit(FEED_BASE_URL, debugClient(context, withAuth = true)).create(MainApi::class.java)

    fun followApi(context: Context): FollowApi =
        retrofit(USER_BASE_URL, debugClient(context, withAuth = true)).create(FollowApi::class.java)

    fun socialApi(context: Context): SocialInteractionApi =
        retrofit(SOCIAL_BASE_URL, debugClient(context, withAuth = true))
            .create(SocialInteractionApi::class.java)

    fun commentApi(context: Context): CommentApi =
        retrofit(SOCIAL_BASE_URL, debugClient(context, withAuth = true))
            .create(CommentApi::class.java)
    fun placeApi(context: Context): PlaceApi =
        retrofit(PLACE_BASE_URL, debugClient(context, withAuth = true))
            .create(PlaceApi::class.java)

    fun roadmapApi(context: Context): RoadmapApi =
        retrofit(PLACE_BASE_URL, debugClient(context, withAuth = true))
            .create(RoadmapApi::class.java)
}
/*
4) Network (공용 Retrofit/OkHttp 팩토리)
무엇을: Interceptor + Authenticator가 장착된 하나의 클라이언트 생성.
왜 이렇게:

모든 API 인스턴스가 같은 정책/타임아웃/리트라이를 공유 → 버그 줄고 유지보수 쉬움.

“리프레시 자동화”가 앱 전역에서 일관되게 동작.
 */