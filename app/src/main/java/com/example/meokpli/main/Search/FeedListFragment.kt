package com.example.meokpli.main.Search

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.auth.Network
import com.example.meokpli.main.SocialInteractionApi
import com.example.meokpli.R
import com.example.meokpli.user.UserApi
import com.example.meokpli.data.remote.response.SearchFeedResponse
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedListFragment : Fragment(R.layout.fragment_search_feed) {
    private var onEditClick: (() -> Unit)? = null

    fun setOnEditClickListener(listener: () -> Unit) {
        onEditClick = listener
    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchFeedAdapter
    private lateinit var socialInteractionApi: SocialInteractionApi
    private lateinit var userApi: UserApi  

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        socialInteractionApi = Network.socialApi(requireContext())
        userApi = Network.userApi(requireContext())
        recyclerView = view.findViewById(R.id.rvMyFeeds)
        adapter = SearchFeedAdapter(
            mutableListOf(),
            onFeedClick = { feedId ->
                // TODO: 상세 게시물 화면 이동
            }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
        //배포 후 설정
//        val categories = userApi.getCategories()
        //renderCategoryChips(view, listOf("전통적인", "카페/디저트", "친구"))
        loadFeeds()

        val editBtn = view.findViewById<TextView>(R.id.btn_edit_region)
        editBtn.setOnClickListener {
            onEditClick?.invoke()  // ✅ NavController 대신 콜백 실행
        }
    }

    private fun loadFeeds(page: Int = 0, size: Int = 10) {
        socialInteractionApi.searchFeed(page, size).enqueue(object : Callback<SearchFeedResponse> {
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
                        Log.d("feeds",feeds.toString())
                        adapter.updateItems(feeds)
                    }
                }
            }

            override fun onFailure(call: Call<SearchFeedResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
    private fun renderCategoryChips(view: View, categories: List<String>) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSelected)
        chipGroup.removeAllViews()

        for (c in categories) {
            val chip = Chip(requireContext()).apply {
                text = c
                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            chipGroup.addView(chip)
        }
    }

}

data class SearchedFeed(
    val feedId: String,
    val photoUrl: String
)