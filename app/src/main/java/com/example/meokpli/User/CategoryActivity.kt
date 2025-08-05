package com.example.meokpli.User

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.meokpli.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class CategoryActivity : AppCompatActivity() {

    private lateinit var chipGroupMood: ChipGroup
    private lateinit var chipGroupFood: ChipGroup
    private lateinit var chipGroupCompanion: ChipGroup
    private lateinit var submitButton: Button

    private val moodList = listOf("전통적인","이색적인","감성적인", "힐링되는", "뷰 맛집", "활기찬", "로컬")
    private val foodList = listOf("분식", "카페/디저트", "치킨", "중식", "한식", "돈까스/회","패스트푸드","족발/보쌈","피자","양식","고기","아시안","도시락","야식","찜/탕")
    private val companionList = listOf("혼밥", "친구", "연인", "가족", "단체","반려동물 동반","동호회")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        chipGroupMood = findViewById(R.id.chipGroupMood)
        chipGroupFood = findViewById(R.id.chipGroupFood)
        chipGroupCompanion = findViewById(R.id.chipGroupCompanion)
        submitButton = findViewById(R.id.submitButton)

        createChips(chipGroupMood, moodList)
        createChips(chipGroupFood, foodList)
        createChips(chipGroupCompanion, companionList)

        submitButton.setOnClickListener {
            val selected = getSelectedChips()
            Log.d("SelectedChips", selected.toString())
            Toast.makeText(this, "선택됨: $selected", Toast.LENGTH_SHORT).show()
            
        }
    }

    private fun createChips(chipGroup: ChipGroup, items: List<String>) {
        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isCheckable = true
                isClickable = true
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
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
}
