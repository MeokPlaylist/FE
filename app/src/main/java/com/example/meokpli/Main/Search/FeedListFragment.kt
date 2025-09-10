package com.example.meokpli.Main.Search

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.SocialInteractionApi
import com.example.meokpli.R
import com.example.meokpli.data.remote.response.SearchFeedResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedListFragment : Fragment(R.layout.fragment_search_feed) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchFeedAdapter
    private lateinit var api: SocialInteractionApi

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = Network.socialApi(requireContext())
        recyclerView = view.findViewById(R.id.rvMyFeeds)
        adapter = SearchFeedAdapter(
            mutableListOf(),
            onFeedClick = { feedId ->
                // 클릭 시 동작
                // 상세 게시물로 이동
            }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        loadFeeds()

        val editBtn = view.findViewById<TextView>(R.id.btn_edit_region)
        editBtn.setOnClickListener {
            findNavController().navigate(R.id.action_feedListFragment_to_searchFeedFragment)
        }
    }

    private fun loadFeeds(page: Int = 0, size: Int = 10) {
        api.searchFeed(page, size).enqueue(object : Callback<SearchFeedResponse> {
            override fun onResponse(
                call: Call<SearchFeedResponse>,
                response: Response<SearchFeedResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val feeds = body.urlsMappedByFeedIds.map { map ->
                            val entry = map.entries.first()
                            SearchedFeed(feedId = entry.key, photoUrl = entry.value)
                        }
                        adapter.updateItems(feeds)
                    }
                }
            }

            override fun onFailure(call: Call<SearchFeedResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}
data class SearchedFeed(
    val feedId: String,
    val photoUrl: String
)