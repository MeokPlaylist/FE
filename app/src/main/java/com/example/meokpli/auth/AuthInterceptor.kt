package com.example.meokpli.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val access = tokenManager.getAccessToken()?.trim()

        val builder = original.newBuilder()

        if (!access.isNullOrEmpty()) {
            // 이미 Authorization가 있다면 제거하고 다시 넣기
            builder.removeHeader("Authorization")

            // Bearer 중복 방지
            val authValue = if (access.startsWith("Bearer ", ignoreCase = true))
                access
            else
                "Bearer $access"

            // addHeader가 아니라 header 사용(중복 방지)
            builder.header("Authorization", authValue)
        }

        return chain.proceed(builder.build())
    }
}

/*
2) AuthInterceptor (요청에 Authorization 자동 부착)
무엇을: 모든 API 요청에 Bearer <access> 헤더 추가.
왜 이렇게:

매 API마다 헤더 붙이는 중복 제거.

토큰이 없을 땐 조용히 패스(공개 API도 동작).
대안: 각 API 호출부에서 직접 헤더 추가 → 중복/누락/실수 위험 큼.
 */