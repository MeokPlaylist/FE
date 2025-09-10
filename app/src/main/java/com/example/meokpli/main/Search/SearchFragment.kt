package com.example.meokpli.main.Search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.meokpli.main.Resettable
import com.example.meokpli.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SearchFragment : Fragment(R.layout.fragment_search), Resettable {
    private lateinit var viewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = SearchPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Search\nUser"
                1 -> "Search\nFeed"
                else -> "Recommend\nRestaurant"
            }
        }.attach()
    }
    override fun resetToDefault() {
        // 항상 첫 탭으로 이동
        viewPager.setCurrentItem(0, false)

        // 현재 표시 중인 프래그먼트 꺼내기
        val currentFragment =
            childFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
        if (currentFragment is Resettable) {
            currentFragment.resetToDefault() // SearchUserFragment까지 초기화
        }
    }
}
