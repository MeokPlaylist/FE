package com.example.meokpli.Auth

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "1a0fd1421e84e625979ad2a917b4e262") // ✅ 네이티브 앱 키
    }
}