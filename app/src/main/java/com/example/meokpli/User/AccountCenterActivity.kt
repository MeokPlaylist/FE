package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.R

class AccountCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_center) // 레이아웃 적용

        // 뒤로가기
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 비밀번호 변경: row 전체(텍스트/화살표 포함) 터치 시 이동
        findViewById<LinearLayout>(R.id.rowChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // 개인정보: row 전체(텍스트/화살표 포함) 터치 시 이동
        findViewById<LinearLayout>(R.id.rowPersonalInfo).setOnClickListener {
            startActivity(Intent(this, AccountInfoActivity::class.java))
        }

    }
}
