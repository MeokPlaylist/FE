package com.example.meokpli.Auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean

class TokenRefreshAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String = "https://meokplaylist.store/auth/"
): Authenticator {

    private val isRefreshing = AtomicBoolean(false)

    // refresh 전용 Retrofit (인터셉터 없이)
    private val refreshApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // 무한 루프 방지: 같은 요청 반복 실패 시 null
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            if (isRefreshing.get()) {
                // 다른 스레드가 갱신중이면 끝날 때까지 실패 응답만 반환 -> OkHttp가 한 번 더 시도하며 최신 토큰으로 인터셉트됨
                return null
            }

            val refreshToken = tokenManager.getRefreshToken() ?: return null
            isRefreshing.set(true)

            val newTokens = runBlocking {
                try {
                    refreshApi.refresh(RefreshRequest(refreshToken))
                } catch (e: Exception) {
                    null
                }
            }

            isRefreshing.set(false)

            if (newTokens == null || newTokens.accessToken.isBlank()) {
                tokenManager.clear()
                return null // 최종 실패 → 앱에서 로그인 화면으로 유도
            }

            // 새 토큰 저장
            tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

            // 실패했던 요청을 새 Access 토큰으로 재시도
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
/*
3) TokenRefreshAuthenticator (401에서 자동 갱신)
무엇을: 응답이 401이면 OkHttp 레벨에서 POST /refresh 호출 → 새 토큰 저장 → 실패했던 요청을 자동 재시도.
왜 이렇게:

Interceptor는 “요청 전/응답 후” 훅이라 리프레시 재시도 루프/순서 관리가 까다로움.

OkHttp Authenticator는 “인증 실패(401)” 상황을 표준적으로 처리하도록 설계됨.

화면/뷰모델 어디서 호출했든 중앙에서 동일 로직 실행(로컬 코드 깔끔).
세부 의도:

responseCount(response) >= 2로 무한 루프 방지.

AtomicBoolean + synchronized로 동시 다발 리프레시 폭주 방지(토큰 스톰).

리프레시 전용 Retrofit은 인터셉터 없이 생성:

오래된 access가 또 붙어 401 반복되는 상황 차단.

Authenticator 내부에서의 재귀 의존 최소화.

runBlocking 사용: OkHttp가 워커 스레드에서 Authenticator를 부르므로 UI 프리즈 없음(메인스레드 불가).
실패 처리: 새 토큰 못 얻으면 저장소 비우고 null 반환 → 재시도 중단 → 화면에서는 로그인 화면 유지/유도.
 */