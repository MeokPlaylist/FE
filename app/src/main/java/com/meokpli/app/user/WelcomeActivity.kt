package com.meokpli.app.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.meokpli.app.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnGoHome = findViewById<MaterialButton>(R.id.btnGoHome)
        btnGoHome.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java)) // ✅ 온보딩으로 진입
            finish()
        }
    }
}