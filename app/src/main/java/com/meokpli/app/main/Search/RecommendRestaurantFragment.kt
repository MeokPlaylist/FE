package com.meokpli.app.main.Search

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.meokpli.app.R
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.meokpli.app.auth.Network
import com.meokpli.app.main.Feed.RegionSelectDialog
import com.meokpli.app.main.RecommendRestaurantRequest
import com.meokpli.app.main.SocialInteractionApi
import com.meokpli.app.main.cityMap
import com.meokpli.app.main.provinceMap
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class RecommendRestaurantFragment : Fragment(R.layout.fragment_recommend_restaurant) {

    private val selectedRegions = mutableListOf<String>()
    private lateinit var recyclerRestaurants: RecyclerView
    private lateinit var adapter: RestaurantAdapter
    private lateinit var api: SocialInteractionApi


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = Network.socialApi(requireContext())
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSelected)

        recyclerRestaurants = view.findViewById(R.id.recycler_restaurants)
        recyclerRestaurants.layoutManager = LinearLayoutManager(requireContext())
        adapter = RestaurantAdapter(emptyList())
        recyclerRestaurants.adapter = adapter

        parentFragmentManager.setFragmentResultListener(
            RegionSelectDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getStringArrayList(RegionSelectDialog.KEY_SELECTED_CODES) ?: arrayListOf()
            selectedRegions.clear()
            selectedRegions.addAll(result)

            renderRegionChips(chipGroup, selectedRegions)

            // ✅ 지역 변경되면 API 다시 호출
            loadRecommendRestaurants(chipGroup)
        }

        view.findViewById<TextView>(R.id.btn_edit_region).setOnClickListener {
            RegionSelectDialog.newInstance(ArrayList(selectedRegions))
                .show(parentFragmentManager, "RegionSelectDialog")
        }
        //처음엔 자기 카테고리로
        initRecommendRestaurants(chipGroup)
    }
    private fun initRecommendRestaurants(chipGroup: ChipGroup) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.getInitRecommendRestaurant() ?: emptyMap()

                val restaurants = response.flatMap { (region, names) ->
                    val (sido, sigungu) = region.split(":", limit = 2)
                    val displaySido = provinceMap.entries.find { it.value == sido }?.key ?: sido
                    val displaySigungu = cityMap.entries.find { it.value == sigungu }?.key ?: sigungu
                    val displayRegion = "$displaySido $displaySigungu"
                    names.map { Restaurant(it, displayRegion) }
                }

                // ✅ selectedRegions를 서버 응답 기반으로 초기화
                selectedRegions.clear()
                selectedRegions.addAll(response.keys.map { englishCode ->
                    // 영어코드("Gyeonggi:Seongnam-si") → 한글코드("경기:성남시")로 역매핑
                    val (sido, sigungu) = englishCode.split(":", limit = 2)
                    val displaySido = provinceMap.entries.find { it.value == sido }?.key ?: sido
                    val displaySigungu = cityMap.entries.find { it.value == sigungu }?.key ?: sigungu
                    "$displaySido:$displaySigungu"
                })

                // ✅ 항상 selectedRegions 기준으로 칩 그림
                renderRegionChips(chipGroup, selectedRegions)

                adapter.updateData(restaurants)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecommendRestaurants(chipGroup: ChipGroup) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = RecommendRestaurantRequest(
                    selectedRegions.map { toEnglishRegion(it) }  // ✅ 한글 → 영어 변환
                )
                val response = api.getRecommendRestaurant(request)
                // Map<String, List<String>> → Restaurant 리스트로 풀기
                val restaurants = response.flatMap { (region, names) ->
                    val (sido, sigungu) = region.split(":", limit = 2)
                    val displaySido = provinceMap.entries.find { it.value == sido }?.key ?: sido
                    val displaySigungu = cityMap.entries.find { it.value == sigungu }?.key ?: sigungu
                    val displayRegion = "$displaySido $displaySigungu"
                    names.map { Restaurant(it, displayRegion) }
                }

                Log.d("restaurants", restaurants.toString())
                if (restaurants.isEmpty()) {
                    adapter.updateData(emptyList())
                } else {
                    adapter.updateData(restaurants)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun toEnglishRegion(code: String): String {
        val (sido, sigungu) = code.split(":", limit = 2)

        val engSido = provinceMap[sido] ?: sido
        val engSigungu = cityMap[sigungu] ?: sigungu

        return "$engSido:$engSigungu"
    }

    private fun renderRegionChips(
        chipGroup: ChipGroup,
        regions: List<String>
    ) {
        chipGroup.removeAllViews()

        regions.forEach { code ->
            val (sido, sigungu) = code.split(":", limit = 2)

            // ✅ 한글 변환 (UI 표시용)
            val displaySido = provinceMap.entries.find { it.value == sido }?.key ?: sido
            val displaySigungu = cityMap.entries.find { it.value == sigungu }?.key ?: sigungu

            val chip = Chip(requireContext()).apply {
                text = "$displaySido $displaySigungu"
                tag = code   // ✅ 원래 code(한글 형태)를 tag에 저장
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    val originalCode = it.tag as String
                    selectedRegions.remove(originalCode)   // ✅ 실제 selectedRegions에서 제거
                    renderRegionChips(chipGroup, selectedRegions)
                    loadRecommendRestaurants(chipGroup)
                }
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(resources.getColorStateList(R.color.selector_chip_text, null))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            chipGroup.addView(chip)
        }
    }
}


