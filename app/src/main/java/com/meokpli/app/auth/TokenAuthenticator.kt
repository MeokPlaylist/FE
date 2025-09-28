package com.meokpli.app.auth

import TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val api: AuthApi,
    private val tokenManager: TokenManager
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val refresh = tokenManager.getRefreshToken() ?: return null
        return try {
            val newAccess = runBlocking {
                api.refresh(RefreshRequest(refresh))
            }

            // 기존 refreshToken은 그대로 유지
            tokenManager.saveTokens(
                newAccess.accessToken,
                refresh
            )

            // 새 accessToken으로 Authorization 헤더 교체
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newAccess.accessToken}")
                .build()
        } catch (e: Exception) {
            null
        }
    }
}

