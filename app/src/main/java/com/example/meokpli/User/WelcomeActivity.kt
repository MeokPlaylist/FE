package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.R
import com.example.meokpli.User.OnboardingActivity
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