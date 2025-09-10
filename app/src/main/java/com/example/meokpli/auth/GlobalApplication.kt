package com.example.meokpli.auth

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "1a0fd1421e84e625979ad2a917b4e262") // ✅ 네이티브 앱 키
        KakaoMapSdk.init(this, "1a0fd1421e84e625979ad2a917b4e262")
    }
}