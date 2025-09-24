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
            val newTokens = runBlocking {
                api.refresh(RefreshRequest(refresh))
            }
            tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        } catch (e: Exception) {
            null
        }
    }
}
