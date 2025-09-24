package com.meokpli.app.auth

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "5e8d5aff3c8ab9542870123abc048c95") // ✅ 네이티브 앱 키
        KakaoMapSdk.init(this, "5e8d5aff3c8ab9542870123abc048c95")
    }
}