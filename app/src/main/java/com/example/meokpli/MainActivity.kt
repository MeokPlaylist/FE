package com.example.meokpli

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val info = intent.getStringExtra("userInfo")

        val textView = TextView(this).apply {
            text = "로그인 성공!\n\n$info"
            textSize = 20f
            setPadding(50, 200, 50, 50)
        }

        setContentView(textView)
    }
}
