package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.Auth.Network
import com.example.meokpli.R
import com.example.meokpli.User.Region.RegionActivity
import com.example.meokpli.User.category.CategoryApi
import com.example.meokpli.User.category.CategorySetUpRequest
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CategoryActivity : AppCompatActivity() {

    private lateinit var chipGroupMood: ChipGroup
    private lateinit var chipGroupFood: ChipGroup
    private lateinit var chipGroupCompanion: ChipGroup
    private lateinit var submitButton: Button
    private lateinit var plusRegionButton: Button

    private val moodList = listOf("전통적인","이색적인","감성적인", "힐링되는", "뷰 맛집", "활기찬", "로컬")
    private val foodList = listOf("분식", "카페/디저트", "치킨", "중식", "한식", "돈까스/회","패스트푸드","족발/보쌈","피자","양식","고기","아시안","도시락","야식","찜/탕")
    private val companionList = listOf("혼밥", "친구", "연인", "가족", "단체","반려동물 동반","동호회")
    // 지역은 RegionActivity에서 고른 결과를 받아 저장한다고 가정 (지금은 빈 배열로 보냄)
    private var selectedRegions: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        chipGroupMood = findViewById(R.id.chipGroupMood)
        chipGroupFood = findViewById(R.id.chipGroupFood)
        chipGroupCompanion = findViewById(R.id.chipGroupCompanion)
        submitButton = findViewById(R.id.submitButton)
        plusRegionButton = findViewById(R.id.PlusRegionButton)

        createChips(chipGroupMood, moodList)
        createChips(chipGroupFood, foodList)
        createChips(chipGroupCompanion, companionList)

        plusRegionButton.setOnClickListener {
            regionLauncher.launch(Intent(this, RegionActivity::class.java))
        }



        submitButton.setOnClickListener {
            val selected = getSelectedChips()
            val request = CategorySetUpRequest(
                moods = selected["분위기"] ?: emptyList(),
                foods = selected["음식"] ?: emptyList(),
                companions = selected["동반자"] ?: emptyList(),
                regions = selectedRegions // 현재는 빈 배열, RegionActivity 연동 시 값 채워짐
            )

            lifecycleScope.launch {
                val api: CategoryApi = Network.categoryApi(this@CategoryActivity)
                runCatching {
                    withContext(Dispatchers.IO) { api.setupCategories(request) }
                }.onSuccess { res ->
                    if (res.success) {
                        Toast.makeText(this@CategoryActivity, "저장 완료!", Toast.LENGTH_SHORT).show()
                        // TODO: 다음 화면으로 이동 (원하는 플로우에 맞춰 라우팅)
                    } else {
                        Toast.makeText(this@CategoryActivity, res.message ?: "저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { e ->
                    Toast.makeText(this@CategoryActivity, "통신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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


    // onCreate() 내에 launcher 등록
    private val regionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val list = result.data?.getStringArrayListExtra(RegionActivity.EXTRA_SELECTED_REGIONS) ?: arrayListOf()
            selectedRegions = list // 예: ["서울|강남구","경기|고양시"]
            Toast.makeText(this, "지역 ${selectedRegions.size}개 선택됨", Toast.LENGTH_SHORT).show()
            // 필요하면 화면 어딘가에 선택된 지역 요약 표시(UI는 자유)
        }
    }

}
