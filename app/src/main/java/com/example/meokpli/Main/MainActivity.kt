package com.example.meokpli.Main

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.setOnItemSelectedListener { item ->
            val msg = when (item.itemId) {
                R.id.nav_home     -> "홈"
                R.id.nav_search   -> "검색"
                R.id.nav_add      -> "게시물 추가"
                R.id.nav_star     -> "찜"
                R.id.nav_profile  -> "내 계정"
                else              -> ""
            }
            Toast.makeText(this, "$msg 탭 클릭", Toast.LENGTH_SHORT).show()
            true   // 선택 유지
        }
    }
}
