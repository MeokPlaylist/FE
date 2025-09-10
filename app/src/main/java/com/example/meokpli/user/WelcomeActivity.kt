package com.example.meokpli.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.main.MainActivity
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnGoHome = findViewById<MaterialButton>(R.id.btnGoHome)
        btnGoHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}