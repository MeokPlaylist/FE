package com.example.meokpli.Main.Search

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meokpli.Auth.Network
import com.example.meokpli.User.Category.CategoryApi
import com.example.meokpli.User.Category.CategorySetUpRequest
import com.example.meokpli.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFeedFragment : Fragment(R.layout.fragment_search_feed_category) {

    private lateinit var chipGroupMood: ChipGroup
    private lateinit var chipGroupFood: ChipGroup
    private lateinit var chipGroupCompanion: ChipGroup
    private lateinit var chipGroupRegions: ChipGroup
    private lateinit var submitButton: Button
    private lateinit var plusRegionButton: Button

    private var selectedRegions: List<String> = emptyList()

    // 카테고리 한글 리스트
    private val moodList = listOf("전통적인","이색적인","감성적인", "힐링되는", "뷰 맛집", "활기찬", "로컬")
    private val foodList = listOf("분식", "카페/디저트", "치킨", "중식", "한식", "돈까스/회","패스트푸드","족발/보쌈","피자","양식","고기","아시안","도시락","야식","찜/탕")
    private val companionList = listOf("혼밥", "친구", "연인", "가족", "단체","반려동물 동반","동호회")

    // 매핑 테이블
    private val moodMap = mapOf(
        "전통적인" to "TRADITIONAL",
        "이색적인" to "UNIQUE",
        "감성적인" to "EMOTIONAL",
        "힐링되는" to "HEALING",
        "뷰 맛집" to "GOODVIEW",
        "활기찬" to "ACTIVITY",
        "로컬" to "LOCAL"
    )
    private val foodMap = mapOf(
        "분식" to "BUNSIK",
        "카페/디저트" to "CAFEDESERT",
        "치킨" to "CHICKEN",
        "중식" to "CHINESE",
        "한식" to "KOREAN",
        "돈까스/회" to "PORK_SASHIMI",
        "패스트푸드" to "FASTFOOD",
        "족발/보쌈" to "JOKBAL_BOSSAM",
        "피자" to "PIZZA",
        "양식" to "WESTERN",
        "고기" to "MEAT",
        "아시안" to "ASIAN",
        "도시락" to "DOSIRAK",
        "야식" to "LATE_NIGHT",
        "찜/탕" to "JJIM_TANG"
    )
    private val companionMap = mapOf(
        "혼밥" to "ALONE",
        "친구" to "FRIEND",
        "연인" to "COUPLE",
        "가족" to "FAMILY",
        "단체" to "GROUP",
        "반려동물 동반" to "WITH_PET",
        "동호회" to "ALUMNI"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroupMood = view.findViewById(R.id.chipGroupMood)
        chipGroupFood = view.findViewById(R.id.chipGroupFood)
        chipGroupCompanion = view.findViewById(R.id.chipGroupCompanion)
        chipGroupRegions = view.findViewById(R.id.chipGroupRegions)
        submitButton = view.findViewById(R.id.submitButton)
        plusRegionButton = view.findViewById(R.id.PlusRegionButton)

        // 칩 생성
        createChips(chipGroupMood, moodList)
        createChips(chipGroupFood, foodList)
        createChips(chipGroupCompanion, companionList)

        submitButton.setOnClickListener {
            if (!hasAllSelected()) {
                Toast.makeText(requireContext(), "분위기·음식·동반자에서 각각 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selected = getSelectedChips()
            val categories = buildCategoryStrings(
                moodsKo = selected["분위기"] ?: emptyList(),
                foodsKo = selected["음식"] ?: emptyList(),
                companionsKo = selected["동반자"] ?: emptyList(),
                regions = selectedRegions
            )

            val request = CategorySetUpRequest(
                categories = categories,
                regions = selectedRegions // region 선택 기능 필요시 매핑 추가
            )
            Log.d("request",request.toString())
            lifecycleScope.launch {
                val api: CategoryApi = Network.categoryApi(requireContext())
                runCatching {
                    withContext(Dispatchers.IO) { api.setupCategories(request) }
                }
                    .onSuccess {
                        Toast.makeText(requireContext(), "카테고리 설정 완료!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_searchFeedFragment_to_feedListFragment)
                    }
                    .onFailure { e ->
                        Toast.makeText(requireContext(), "카테고리 설정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // --- util 함수들 ---
    private fun createChips(chipGroup: ChipGroup, items: List<String>) {
        for (item in items) {
            val chip = Chip(requireContext()).apply {
                text = item
                isCheckable = true
                isClickable = true
                isCloseIconVisible = false
                chipIcon = null
                checkedIcon = null
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            chipGroup.addView(chip)
        }
    }

    private fun getSelectedChips(): Map<String, List<String>> {
        fun selectedFrom(group: ChipGroup): List<String> {
            return (0 until group.childCount)
                .mapNotNull { group.getChildAt(it) as? Chip }
                .filter { it.isChecked }
                .map { it.text.toString() }
        }
        return mapOf(
            "분위기" to selectedFrom(chipGroupMood),
            "음식" to selectedFrom(chipGroupFood),
            "동반자" to selectedFrom(chipGroupCompanion)
        )
    }

    private fun hasAllSelected(): Boolean {
        return chipGroupMood.checkedChipIds.isNotEmpty() &&
                chipGroupFood.checkedChipIds.isNotEmpty() &&
                chipGroupCompanion.checkedChipIds.isNotEmpty()
    }

    private fun buildCategoryStrings(
        moodsKo: List<String>,
        foodsKo: List<String>,
        companionsKo: List<String>,
        regions: List<String>
    ): List<String> {
        val result = mutableListOf<String>()
        moodsKo.mapNotNull { moodMap[it] }.forEach { result += "moods:$it" }
        foodsKo.mapNotNull { foodMap[it] }.forEach { result += "foods:$it" }
        companionsKo.mapNotNull { companionMap[it] }.forEach { result += "companions:$it" }
        return result
    }
}
