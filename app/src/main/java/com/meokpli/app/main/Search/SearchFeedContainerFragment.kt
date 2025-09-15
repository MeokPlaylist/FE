package com.meokpli.app.main.Search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.meokpli.app.R

class SearchFeedContainerFragment : Fragment(R.layout.fragment_feed_container) {

    private var selectedCategories: MutableList<String> = mutableListOf()
    private var selectedRegions: MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            if (selectedCategories.isNotEmpty() || selectedRegions.isNotEmpty()) {
                showFeedList()
            } else {
                showCategorySetup()
            }
        }
    }

    fun showCategorySetup() {
        val searchFeedFragment = SearchFeedFragment().apply {
            setOnCompleteListener { categories, regions ->
                selectedCategories.clear()
                selectedCategories.addAll(categories)
                selectedRegions.clear()
                selectedRegions.addAll(regions)
                showFeedList()
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.feed_container, searchFeedFragment)
            .commit()
    }

    fun showFeedList() {
        val feedListFragment = FeedListFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList("categories", ArrayList(selectedCategories))
                putStringArrayList("regions", ArrayList(selectedRegions))
            }
            setOnCategoriesChangedListener { updatedCategories, updatedRegions ->
                selectedCategories.clear()
                selectedCategories.addAll(updatedCategories)
                selectedRegions.clear()
                selectedRegions.addAll(updatedRegions)
                showFeedList()
            }
            setOnEditClickListener {
                showCategorySetup()
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.feed_container, feedListFragment)
            .commit()
    }
}
