package com.example.meokpli.Main

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.meokpli.R

class MainActivity : AppCompatActivity() {

    enum class Tab { HOME, SEARCH, FEED, STAR, PROFILE }

    // 탭/아이콘/버튼
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

    // 탭 상태
    private var currentTab: Tab = Tab.HOME
    private val tabHistory = ArrayDeque<Tab>() // 최근 탭 스택

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 네비/아이콘 바인딩
        navHome = findViewById(R.id.nav_home)
        navSearch = findViewById(R.id.nav_search)
        navFeed = findViewById(R.id.nav_feed)
        navStar = findViewById(R.id.nav_star)
        navProfile = findViewById(R.id.nav_profile)

        iconHome = findViewById(R.id.icon_home)
        iconSearch = findViewById(R.id.icon_search)
        iconFeed = findViewById(R.id.icon_feed)
        iconStar = findViewById(R.id.icon_star)
        iconProfile = findViewById(R.id.icon_profile)

        // 초기 탭 진입 (한 번만)
        tabHistory.clear()
        tabHistory.addLast(Tab.HOME)
        currentTab = Tab.HOME
        replaceRoot(HomeFragment())
        setActiveIcon(iconHome)

        // 탭 클릭 → switchTab
        navHome.setOnClickListener { switchTab(Tab.HOME) }
        navSearch.setOnClickListener { switchTab(Tab.SEARCH) }
        navFeed.setOnClickListener { switchTab(Tab.FEED) }
        navStar.setOnClickListener { switchTab(Tab.STAR) }
        navProfile.setOnClickListener { switchTab(Tab.PROFILE) }
    }

    /** 상세 화면 진입: 백스택에 쌓임 (프로필→팔로워목록 등) */
    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /** 직전 상태로: 상세면 pop, 아니면 직전 탭, 아니면 종료 */
    fun goToLastTab() {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            return
        }
        if (tabHistory.size <= 1) {
            if (currentTab != Tab.HOME) {
                switchTab(Tab.HOME, record = false)
            } else {
                finish()
            }
            return
        }
        // 현재 탭 제거 후 직전 탭으로
        tabHistory.removeLast()
        val prev = tabHistory.last()
        switchTab(prev, record = false)
    }

    /** 탭 전환: 루트로 교체(백스택 비움) + 아이콘 하이라이트 */
    fun switchTab(tab: Tab, record: Boolean = true) {
        if (tab == currentTab) return

        // 상세 스택 초기화 (탭 전환 시)
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        currentTab = tab
        if (record) {
            if (tabHistory.isEmpty() || tabHistory.last() != tab) {
                tabHistory.addLast(tab)
                if (tabHistory.size > 20) tabHistory.removeFirst()
            }
        }

        val fragment = when (tab) {
            Tab.HOME -> HomeFragment()
            Tab.SEARCH -> SearchFragment()
            Tab.FEED -> FeedFragment()
            Tab.STAR -> StarFragment()
            Tab.PROFILE -> ProfileFragment()
        }
        replaceRoot(fragment)

        when (tab) {
            Tab.HOME -> setActiveIcon(iconHome)
            Tab.SEARCH -> setActiveIcon(iconSearch)
            Tab.FEED -> setActiveIcon(iconFeed)
            Tab.STAR -> setActiveIcon(iconStar)
            Tab.PROFILE -> setActiveIcon(iconProfile)
        }
    }

    /** 루트 프래그먼트 교체 (백스택 사용 안 함) */
    private fun replaceRoot(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    /** 아이콘 강조(현재 알파 방식) */
    private fun setActiveIcon(activeIcon: ImageView) {
        iconHome.alpha = 0.5f
        iconSearch.alpha = 0.5f
        iconFeed.alpha = 0.5f
        iconStar.alpha = 0.5f
        iconProfile.alpha = 0.5f
        activeIcon.alpha = 1.0f
    }

    // 하드웨어 뒤로 버튼도 탭/백스택 정책으로
    override fun onBackPressed() {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        } else if (currentTab != Tab.HOME) {
            switchTab(Tab.HOME, record = false)
        } else {
            super.onBackPressed()
        }
    }
}
