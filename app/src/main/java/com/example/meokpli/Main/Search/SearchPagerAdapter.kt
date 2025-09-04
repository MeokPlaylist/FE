package com.example.meokpli.Main.Search

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SearchUserFragment()
            1 -> SearchFeedFragment()
            else -> RecommendRestaurantFragment()
        } as Fragment
    }
}
