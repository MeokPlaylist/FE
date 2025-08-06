package com.example.meokpli

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.meokpli.Main.*

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navSearch: LinearLayout
    private lateinit var navFeed: LinearLayout
    private lateinit var navStar: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconFeed: ImageView
    private lateinit var iconStar: ImageView
    private lateinit var iconProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // ViewBinding 사용 안함

        // 네비게이션 버튼 초기화
        navHome = findViewById(R.id.nav_home)
        navSearch = findViewById(R.id.nav_search)
        navFeed = findViewById(R.id.nav_feed)
        navStar = findViewById(R.id.nav_star)
        navProfile = findViewById(R.id.nav_profile)

        // 아이콘 초기화
        iconHome = findViewById(R.id.icon_home)
        iconSearch = findViewById(R.id.icon_search)
        iconFeed = findViewById(R.id.icon_feed)
        iconStar = findViewById(R.id.icon_star)
        iconProfile = findViewById(R.id.icon_profile)

        // 초기 화면 - HomeFragment
        replaceFragment(HomeFragment())
        setActiveIcon(iconHome)

        // 클릭 이벤트 연결
        navHome.setOnClickListener {
            replaceFragment(HomeFragment())
            setActiveIcon(iconHome)
        }
        navSearch.setOnClickListener {
            replaceFragment(SearchFragment())
            setActiveIcon(iconSearch)
        }
        navFeed.setOnClickListener {
            replaceFragment(FeedFragment())
            setActiveIcon(iconFeed)
        }
        navStar.setOnClickListener {
            replaceFragment(StarFragment())
            setActiveIcon(iconStar)
        }
        navProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
            setActiveIcon(iconProfile)
        }
    }

    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    // 선택된 아이콘 강조 함수
    private fun setActiveIcon(activeIcon: ImageView) {
        // 모든 아이콘 초기화 (회색으로)
        iconHome.alpha = 0.5f
        iconSearch.alpha = 0.5f
        iconFeed.alpha = 0.5f
        iconStar.alpha = 0.5f
        iconProfile.alpha = 0.5f

        // 선택된 아이콘만 활성화 (검정색 진하게)
        activeIcon.alpha = 1.0f
    }
}
