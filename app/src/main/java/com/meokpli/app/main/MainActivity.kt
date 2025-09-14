package com.meokpli.app.main

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.meokpli.app.R

class MainActivity : AppCompatActivity() {
    private lateinit var navHome: LinearLayout
    private lateinit var navSearch: LinearLayout
    private lateinit var navFeed: ImageButton
    private lateinit var navStar: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconStar: ImageView
    private lateinit var iconProfile: ImageView

    private lateinit var nav: NavController

    // 뒤로가기 두 번 눌렀는지 확인용
    private var backPressedTime: Long = 0
    private val BACK_PRESS_INTERVAL = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) 뷰 바인딩
        navHome = findViewById(R.id.nav_home)
        navSearch = findViewById(R.id.nav_search)
        navFeed = findViewById(R.id.nav_feed)
        navStar = findViewById(R.id.nav_star)
        navProfile = findViewById(R.id.nav_profile)

        iconHome = findViewById(R.id.icon_home)
        iconSearch = findViewById(R.id.icon_search)
        iconStar = findViewById(R.id.icon_star)
        iconProfile = findViewById(R.id.icon_profile)

        // 2) NavController는 NavHostFragment에서 직접 꺼낸다 (findNavController() 대신)
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        nav = navHost.navController

        // 3) 목적지 변경될 때 아이콘 상태 갱신
        nav.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment   -> setActiveIcon(iconHome)
                R.id.searchFragment -> setActiveIcon(iconSearch)
                R.id.starFragment   -> setActiveIcon(iconStar)
                R.id.profileFragment-> setActiveIcon(iconProfile)
            }
        }

        navHome.setOnClickListener {
            handleTabReselection(R.id.homeFragment)
        }
        navSearch.setOnClickListener {
            handleTabReselection(R.id.searchFragment)
        }
        navFeed.setOnClickListener {
            handleTabReselection(R.id.feedFragment)
        }
        navStar.setOnClickListener {
            handleTabReselection(R.id.starFragment)
        }
        navProfile.setOnClickListener {
            handleTabReselection(R.id.profileFragment)
        }

        // 5) 디바이스 뒤로가기
        onBackPressedDispatcher.addCallback(this) {
            handleSystemBack()
        }
    }
    private fun handleTabReselection(destId: Int) {
        val currentId = nav.currentDestination?.id

        // ✅ 같은 탭 (루트 or 하위 화면)
        if (isInSameTab(currentId, destId)) {
            // 현재 Search 트리에 있음 → 루트까지 popBackStack
            val popped = nav.popBackStack(destId, false)
            val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHost.view?.post {
                val rootFragment = navHost.childFragmentManager.fragments.firstOrNull()
                if (rootFragment is Resettable) {
                    rootFragment.resetToDefault() // 검색어 초기화
                }
            }
            if (!popped) {
                // 혹시 실패하면 안전하게 navigate
                nav.navigate(destId)
            }
        } else {
            // ✅ 다른 탭 → 상태 유지하며 navigate
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(nav.graph.startDestinationId, inclusive = false, saveState = true)
                .build()
            nav.navigate(destId, null, options)
        }
    }

    // 현재 목적지가 해당 탭(루트 or 하위)에 속하는지 판별
    private fun isInSameTab(currentId: Int?, tabRootId: Int): Boolean {
        if (currentId == null) return false

        return when (tabRootId) {
            R.id.homeFragment -> currentId == R.id.homeFragment
            R.id.searchFragment -> currentId == R.id.searchFragment || currentId == R.id.otherProfileFragment
            R.id.feedFragment -> currentId == R.id.feedFragment
            R.id.starFragment -> currentId == R.id.starFragment
            R.id.profileFragment -> {
                currentId == R.id.profileFragment ||
                        currentId == R.id.fragmentSetting ||
                        currentId == R.id.followListFragment
            }
            else -> false
        }
    }

    fun handleSystemBack() {
        val destId = nav.currentDestination?.id

        if (nav.previousBackStackEntry != null && !isTopLevel(destId)) {
            nav.popBackStack()
            return
        }
        if (isTopLevel(destId) && destId != R.id.homeFragment) {
            nav.navigate(R.id.homeFragment)
            return
        }
        // 홈 화면에서 종료 시 두 번 눌러야 꺼지도록
        if (System.currentTimeMillis() - backPressedTime < BACK_PRESS_INTERVAL) {
            finish()
        } else {
            Toast.makeText(this, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()


        }
    }

    private fun isTopLevel(destId: Int?): Boolean {
        return when (destId) {
            R.id.homeFragment, R.id.searchFragment, R.id.feedFragment,
            R.id.starFragment, R.id.profileFragment -> true
            else -> false
        }
    }
    // 아이콘 강조
    private fun setActiveIcon(activeIcon: ImageView) {
        iconHome.alpha = 0.5f
        iconSearch.alpha = 0.5f
        iconStar.alpha = 0.5f
        iconProfile.alpha = 0.5f
        activeIcon.alpha = 1.0f
    }
}

interface Resettable {
    fun resetToDefault()
}
