package com.example.meokpli.Main

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
import com.example.meokpli.R

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

        // 4) 탭 클릭: 같은 목적지면 무시, 아니면 이동
        val singleTopPopToStart = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(nav.graph.startDestinationId, inclusive = false, saveState = true)
            .build()

        navHome.setOnClickListener {
            navigateTopLevel(R.id.homeFragment, singleTopPopToStart)
        }
        navSearch.setOnClickListener {
            navigateTopLevel(R.id.searchFragment, singleTopPopToStart)
        }
        navFeed.setOnClickListener {
            navigateTopLevel(R.id.feedFragment, singleTopPopToStart)
        }
        navStar.setOnClickListener {
            navigateTopLevel(R.id.starFragment, singleTopPopToStart)
        }
        navProfile.setOnClickListener {
            navigateTopLevel(R.id.profileFragment, singleTopPopToStart)
        }

        // 5) 디바이스 뒤로가기
        onBackPressedDispatcher.addCallback(this) {
            handleSystemBack()
        }
    }

    private fun navigateTopLevel(destId: Int, options: NavOptions) {
        if (nav.currentDestination?.id != destId) {
            nav.navigate(destId, null, options)
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