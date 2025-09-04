package com.example.meokpli.Main.Search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meokpli.R
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import android.widget.TextView
import com.example.meokpli.Main.Feed.RegionSelectDialog
import com.google.android.material.chip.Chip

class RecommendRestaurantFragment : Fragment(R.layout.fragment_recommend_restaurant) {

    private val selectedRegions = mutableListOf<String>() // "서울:강남구" 형태

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSelected)

        //  다이얼로그 결과 수신
        parentFragmentManager.setFragmentResultListener(
            RegionSelectDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getStringArrayList(RegionSelectDialog.KEY_SELECTED_CODES) ?: arrayListOf()
            selectedRegions.clear()
            selectedRegions.addAll(result)

            // 칩 렌더링
            renderRegionChips(chipGroup, selectedRegions)
        }

        // 수정하기" 버튼 → 다이얼로그 열기
        view.findViewById<TextView>(R.id.btn_edit_region).setOnClickListener {
            RegionSelectDialog.newInstance(ArrayList(selectedRegions))
                .show(parentFragmentManager, "RegionSelectDialog")
        }

        // 음식점 리스트 (RecyclerView)
        val recyclerRestaurants = view.findViewById<RecyclerView>(R.id.recycler_restaurants)
        recyclerRestaurants.layoutManager = LinearLayoutManager(requireContext())
        recyclerRestaurants.adapter = RestaurantAdapter(
            listOf(
                Restaurant("삼겹살 맛집", "서울 강남구"),
                Restaurant("김치찌개 골목", "서울 마포구"),
                Restaurant("파스타 레스토랑", "서울 용산구")
            )
        )
    }

    /** ChipGroup 기반 지역 칩 렌더링 (개수 표시 X) */
    /** ChipGroup 기반 지역 칩 렌더링 */
    private fun renderRegionChips(
        chipGroup: ChipGroup,
        regions: List<String>
    ) {
        chipGroup.removeAllViews()

        regions.forEach { code ->
            val (sido, sigungu) = code.split(":", limit = 2)
            val chip = Chip(requireContext()).apply {
                // ✅ "서울 강서구" 같이 보이도록 수정
                text = "$sido $sigungu"
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    (regions as MutableList).remove(code)
                    renderRegionChips(chipGroup, regions)
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
