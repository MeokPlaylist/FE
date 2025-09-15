package com.meokpli.app.main.Search

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.auth.Network
import com.meokpli.app.main.SocialInteractionApi
import com.meokpli.app.R
import com.meokpli.app.data.remote.response.SearchFeedResponse
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.meokpli.app.data.remote.request.FeedSearchRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedListFragment : Fragment(R.layout.fragment_search_feed) {
    private var onEditClick: (() -> Unit)? = null
    private var onCategoriesChanged: ((List<String>, List<String>) -> Unit)? = null

    fun setOnEditClickListener(listener: () -> Unit) {
        onEditClick = listener
    }

    fun setOnCategoriesChangedListener(listener: (List<String>, List<String>) -> Unit) {
        onCategoriesChanged = listener
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchFeedAdapter
    private lateinit var socialInteractionApi: SocialInteractionApi

    private var categories: MutableList<String> = mutableListOf()
    private var regions: MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        socialInteractionApi = Network.socialApi(requireContext())
        recyclerView = view.findViewById(R.id.rvMyFeeds)
        adapter = SearchFeedAdapter(mutableListOf(), onFeedClick = { feedId -> /* TODO */ })
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        categories = arguments?.getStringArrayList("categories")?.toMutableList() ?: mutableListOf()
        regions = arguments?.getStringArrayList("regions")?.toMutableList() ?: mutableListOf()

        renderCategoryChips(view, categories, regions)
        loadFeeds(categories, regions)

        val editBtn = view.findViewById<TextView>(R.id.btn_edit)
        editBtn.setOnClickListener { onEditClick?.invoke() }
    }

    private fun loadFeeds(categories: List<String>, regions: List<String>, page: Int = 0, size: Int = 10) {
        val request = FeedSearchRequest(categories, regions)
        socialInteractionApi.searchFeed(request, page, size).enqueue(object : Callback<SearchFeedResponse> {
            override fun onResponse(call: Call<SearchFeedResponse>, response: Response<SearchFeedResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val feeds = body.urlsMappedByFeedIds.map { map ->
                            val entry = map.entries.first()
                            SearchedFeed(feedId = entry.key, photoUrl = entry.value)
                        }
                        Log.d("feeds", feeds.toString())
                        adapter.updateItems(feeds)
                    }
                }
            }

            override fun onFailure(call: Call<SearchFeedResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun renderCategoryChips(view: View, categories: List<String>, regions: List<String>) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSelected)
        chipGroup.removeAllViews()

        // 역매핑 테이블 (영어 코드 -> 한글)
        val reverseMoodMap = mapOf(
            "TRADITIONAL" to "전통적인", "UNIQUE" to "이색적인", "EMOTIONAL" to "감성적인",
            "HEALING" to "힐링되는", "GOODVIEW" to "뷰 맛집", "ACTIVITY" to "활기찬", "LOCAL" to "로컬"
        )
        val reverseFoodMap = mapOf(
            "BUNSIK" to "분식","CAFE_DESSERT" to "카페/디저트","CHICKEN" to "치킨","CHINESE" to "중식",
            "KOREAN" to "한식","PORK_SASHIMI" to "돈까스/회","FASTFOOD" to "패스트푸드","JOKBAL_BOSSAM" to "족발/보쌈",
            "PIZZA" to "피자","WESTERN" to "양식","MEAT" to "고기","ASIAN" to "아시안","DOSIRAK" to "도시락",
            "LATE_NIGHT" to "야식","JJIM_TANG" to "찜/탕"
        )
        val reverseCompanionMap = mapOf(
            "ALONE" to "혼밥","FRIEND" to "친구","COUPLE" to "연인","FAMILY" to "가족",
            "GROUP" to "단체","WITH_PET" to "반려동물 동반","ALUMNI" to "동호회"
        )

        fun addChip(original: String, isRegion: Boolean) {
            val displayText = if (isRegion) {
                // 지역: "서울:관악구" → "서울 관악구"
                original.replace(":", " ")
            } else {
                // 카테고리: "foods:CAFE_DESSERT" → "카페/디저트"
                val parts = original.split(":")
                if (parts.size == 2) {
                    val type = parts[0]
                    val value = parts[1]
                    when (type) {
                        "moods" -> reverseMoodMap[value] ?: value
                        "foods" -> reverseFoodMap[value] ?: value
                        "companions" -> reverseCompanionMap[value] ?: value
                        else -> value
                    }
                } else {
                    original
                }
            }

            val chip = Chip(requireContext()).apply {
                text = displayText
                isCheckable = false
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    if (isRegion) {
                        val updated = this@FeedListFragment.regions.toMutableList()
                        updated.remove(original)
                        this@FeedListFragment.regions = updated
                    } else {
                        val updated = this@FeedListFragment.categories.toMutableList()
                        updated.remove(original)
                        this@FeedListFragment.categories = updated
                    }
                    renderCategoryChips(view, this@FeedListFragment.categories, this@FeedListFragment.regions)
                    loadFeeds(this@FeedListFragment.categories, this@FeedListFragment.regions)
                    onCategoriesChanged?.invoke(this@FeedListFragment.categories, this@FeedListFragment.regions)
                }
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            chipGroup.addView(chip)
        }

        categories.forEach { addChip(it, false) }
        regions.forEach { addChip(it, true) }
    }
}

data class SearchedFeed(
    val feedId: Long,
    val photoUrl: String
)
