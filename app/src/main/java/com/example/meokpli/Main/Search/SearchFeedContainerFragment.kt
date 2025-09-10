package com.example.meokpli.Main.Search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.meokpli.R

class SearchFeedContainerFragment : Fragment(R.layout.fragment_feed_container) {

    private var isCategorySet = false // 서버 상태나 SharedPref로 체크 가능

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            if (isCategorySet) {
                showFeedList()
            } else {
                showCategorySetup()
            }
        }
    }

    fun showCategorySetup() {
        val searchFeedFragment = SearchFeedFragment().apply {
            setOnCompleteListener {
                isCategorySet = true
                showFeedList()
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.feed_container, searchFeedFragment)
            .commit()
    }

    fun showFeedList() {
        val feedListFragment = FeedListFragment().apply {
            setOnEditClickListener {
                showCategorySetup()
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.feed_container, feedListFragment)
            .commit()
    }
}
