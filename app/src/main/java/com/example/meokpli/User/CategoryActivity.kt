package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.R
import com.example.meokpli.User.Region.RegionActivity
import com.example.meokpli.User.Category.*
import com.example.meokpli.User.Category.CategorySetUpRequest
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CategoryActivity : AppCompatActivity() {

    private lateinit var chipGroupMood: ChipGroup
    private lateinit var chipGroupFood: ChipGroup
    private lateinit var chipGroupCompanion: ChipGroup
    private lateinit var chipGroupRegions: ChipGroup
    // 이미 있는 selectedRegions: List<String>  // 예: ["서울|강남구","경기|고양시"]
    private lateinit var submitButton: Button
    private lateinit var plusRegionButton: Button



    private val moodList = listOf("전통적인","이색적인","감성적인", "힐링되는", "뷰 맛집", "활기찬", "로컬")
    private val foodList = listOf("분식", "카페/디저트", "치킨", "중식", "한식", "돈까스/회","패스트푸드","족발/보쌈","피자","양식","고기","아시안","도시락","야식","찜/탕")
    private val companionList = listOf("혼밥", "친구", "연인", "가족", "단체","반려동물 동반","동호회")
    // 지역은 RegionActivity에서 고른 결과를 받아 저장한다고 가정 (지금은 빈 배열로 보냄)
    private var selectedRegions: List<String> = emptyList()

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

    private fun translate(list: List<String>, dict: Map<String, String>): List<String> {
        return list.mapNotNull { dict[it] } // 매핑 없는 값은 버림(null 제외)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        submitButton = findViewById(R.id.submitButton)
        plusRegionButton = findViewById(R.id.PlusRegionButton)

        submitButton.isEnabled = true
        submitButton.isClickable = true

        chipGroupMood = findViewById(R.id.chipGroupMood)
        chipGroupFood = findViewById(R.id.chipGroupFood)
        chipGroupCompanion = findViewById(R.id.chipGroupCompanion)
        chipGroupRegions = findViewById(R.id.chipGroupRegions)

        createChips(chipGroupMood, moodList)
        createChips(chipGroupFood, foodList)
        createChips(chipGroupCompanion, companionList)

        plusRegionButton.setOnClickListener {
            val intent = Intent(this, RegionActivity::class.java).apply {
                putStringArrayListExtra(
                    RegionActivity.EXTRA_PRESELECTED, ArrayList(selectedRegions)
                )
            }
            regionLauncher.launch(intent)
        }

        submitButton.setOnClickListener {
            if (!hasAllSelected()) {
                Toast.makeText(this, "분위기·음식·동반자에서 각각 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selected = getSelectedChips()

            val categories = buildCategoryStrings(
                moodsKo = selected["분위기"] ?: emptyList(),
                foodsKo = selected["음식"] ?: emptyList(),
                companionsKo = selected["동반자"] ?: emptyList(),
                regions = selectedRegions
            )

            val request = CategorySetUpRequest(categories = categories)
            Log.d("req",request.toString())
            lifecycleScope.launch {
                val api: CategoryApi = Network.categoryApi(this@CategoryActivity)
                runCatching {
                    withContext(Dispatchers.IO) { api.setupCategories(request) }
                }
                    .onSuccess { res ->
                        // Category API 성공 시 User API 호출
                        val userApi = Network.userApi(this@CategoryActivity)

                        runCatching {
                            withContext(Dispatchers.IO) { userApi.newBCheck() }
                        }
                            .onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@CategoryActivity, "카테고리 설정 완료!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@CategoryActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .onFailure { e ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@CategoryActivity, "UserApi 호출 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .onFailure { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CategoryActivity, "카테고리 설정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun buildCategoryStrings(
        moodsKo: List<String>,
        foodsKo: List<String>,
        companionsKo: List<String>,
        regions: List<String>
    ): List<String> {
        val result = mutableListOf<String>()
        translate(moodsKo, moodMap).forEach { result += "moods:$it" }
        translate(foodsKo, foodMap).forEach { result += "foods:$it" }
        translate(companionsKo, companionMap).forEach { result += "companions:$it" }
        // 지역도 같은 규칙이면:
        regions.forEach { result += "regions:$it" }   // 필요 없으면 이 줄 제거
        return result
    }

    private fun hasAllSelected(): Boolean {
        return chipGroupMood.checkedChipIds.isNotEmpty() &&
                chipGroupFood.checkedChipIds.isNotEmpty() &&
                chipGroupCompanion.checkedChipIds.isNotEmpty()
    }

    private fun createChips(chipGroup: ChipGroup, items: List<String>) {
        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isCheckable = true
                isClickable = true
                isCloseIconVisible = false
                chipIcon = null
                checkedIcon = null
                // 배경/텍스트 색 상태 셀렉터
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))

                // 테두리 색/두께 (선택 전 0.75dp 느낌, 선택 후 #C64132)
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                // MaterialChip 스타일 보정
                isCloseIconVisible = false
                isFocusable = false
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

    private fun renderSelectedRegions() {
        chipGroupRegions.removeAllViews()

        selectedRegions.forEach { regionCode ->
            // 화면 표시는 "서울 강남구"처럼, 서버 전송은 원본 그대로 유지
            val label = regionCode.replace("|", " ")

            val chip = Chip(this).apply {
                text = label
                isCheckable = false
                isClickable = true
                isCloseIconVisible = true         // ⨯ 표시
                chipIcon = null
                checkedIcon = null

                // 기존 칩들과 동일한 스타일
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                // 삭제 핸들러
                setOnCloseIconClickListener {
                    // 리스트에서 제거하고 다시 렌더
                    selectedRegions = selectedRegions.filterNot { it == regionCode }
                    renderSelectedRegions()
                }
            }
            chipGroupRegions.addView(chip)
        }
    }

    // onCreate() 내에 launcher 등록
    private val regionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val list = result.data
                ?.getStringArrayListExtra(RegionActivity.EXTRA_SELECTED_REGIONS)
                ?: return@registerForActivityResult   // null이면 기존 유지

            // 비우기로 확정한 경우만 빈 리스트 수용하고 싶다면 조건 추가 가능
            selectedRegions = list
            renderSelectedRegions()
            Toast.makeText(this, "지역 ${selectedRegions.size}개 선택됨", Toast.LENGTH_SHORT).show()
        }
        // RESULT_CANCELED면 아무 것도 하지 않음 → 기존 선택 유지
    }

}
