package com.meokpli.app.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meokpli.app.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        //상태바 표시
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_welcome)

        val btnGoHome = findViewById<MaterialButton>(R.id.btnGoHome)
        btnGoHome.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java)) // ✅ 온보딩으로 진입
            finish()
        }
    }
}