package com.example.meokpli.Auth

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("meokpli_prefs", Context.MODE_PRIVATE)

    fun saveTokens(access: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }

    fun setKeepLogin(keep: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP, keep).apply()
    }
    fun isKeepLogin(): Boolean = prefs.getBoolean(KEY_KEEP, false)

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_KEEP = "keep_login"
    }
}
/*
1) TokenManager (SharedPreferences 저장소)
무엇을: accessToken, refreshToken, ‘로그인 유지’ 여부(keep)를 저장/조회/삭제.
왜 이렇게:

액세스 토큰은 짧은 수명, 리프레시는 길게: 유지의 핵심은 refreshToken 존재 여부라서 둘 다 저장해야 함.

앱 어디에서나 토큰을 쉽게 꺼내 쓰고, 로그아웃 시 한 번에 지우기 쉬움.
대안: 보안이 더 필요하면 EncryptedSharedPreferences로 교체.
 */