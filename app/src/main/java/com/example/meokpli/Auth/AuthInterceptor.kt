package com.example.meokpli.Auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val access = tokenManager.getAccessToken()

        val newReq = if (!access.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $access")
                .build()
        } else original

        return chain.proceed(newReq)
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