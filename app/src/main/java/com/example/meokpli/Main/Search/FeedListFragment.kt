package com.example.meokpli.Main.Search

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.example.meokpli.feed.FeedAdapter

class FeedListFragment : Fragment(R.layout.fragment_search_feed) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvMyFeeds)
        adapter = FeedAdapter(emptyList())
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        loadFeeds()

        val editBtn = view.findViewById<TextView>(R.id.btn_edit_region)
        editBtn.setOnClickListener {
            // Navigation Component 사용
            findNavController().navigate(R.id.action_feedListFragment_to_searchFeedFragment)
        }
    }

    private fun loadFeeds() {
        // TODO: 서버에서 카테고리 기반 피드 가져오기
        val dummy = listOf(
            Feed("카페 A", "https://placehold.co/300x300"),
            Feed("분위기 좋은 술집", "https://placehold.co/300x300"),
            Feed("한식 맛집", "https://placehold.co/300x300")
        )
        adapter.updateItems(dummy)
    }
}
