package com.example.meokpli.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.meokpli.main.Feed.RegionSelectDialog
import com.example.meokpli.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiProvider {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // build.gradle 에 buildConfigField 로 BASE_URL 넣어두면 좋음
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://meokplaylist.com/") // 예: "https://api.meokplaylist.com/"
            .client(client)
            .build() // 멀티파트만 쓰면 컨버터 없어도 OK. JSON 쓰면 Gson 추가
    }

    val api: MainApi by lazy { retrofit.create(MainApi::class.java) }
}

class CategorySelectDialog : DialogFragment() {
    //예외처리용
    private val TAG = "CategorySelectDialog"

    companion object {
        const val REQUEST_KEY = "category_result"
        const val KEY_MOODS = "moods"                 // 라벨 리스트 (UI 미리보기용)
        const val KEY_FOODS = "foods"
        const val KEY_COMPANIONS = "companions"
        const val KEY_PAYLOAD = "payload"             // 서버 전송용 "moods:CODE" 등

        private const val STATE_REGIONS = "state_regions" //내부상태 저장용

        fun newInstance(
            preMoods: ArrayList<String> = arrayListOf(),
            preFoods: ArrayList<String> = arrayListOf(),
            preComps: ArrayList<String> = arrayListOf(),
        ) = CategorySelectDialog().apply {
            arguments = bundleOf(
                KEY_MOODS to preMoods,
                KEY_FOODS to preFoods,
                KEY_COMPANIONS to preComps
            )
        }
    }

    // 지역 코드는 payload용만 보관 (예: "서울|강남구")
    private var selectedRegionCodes = arrayListOf<String>()

    private data class CatItem(val label: String, val code: String)

    private val moodItems = listOf(
        CatItem("전통적인","TRADITIONAL"),
        CatItem("이색적인","UNIQUE"),
        CatItem("감성적인","EMOTIONAL"),
        CatItem("힐링되는","HEALING"),
        CatItem("뷰 맛집","GOODVIEW"),
        CatItem("활기찬","ACTIVITY"),
        CatItem("로컬","LOCAL")
    )
    private val foodItems = listOf(
        CatItem("분식","BUNSIK"), CatItem("카페/디저트","CAFEDESERT"),
        CatItem("치킨","CHICKEN"), CatItem("중식","CHINESE"),
        CatItem("한식","KOREAN"), CatItem("돈까스/회","PORK_SASHIMI"),
        CatItem("패스트푸드","FASTFOOD"), CatItem("족발/보쌈","JOKBAL_BOSSAM"),
        CatItem("피자","PIZZA"), CatItem("양식","WESTERN"),
        CatItem("고기","MEAT"), CatItem("아시안","ASIAN"),
        CatItem("도시락","DOSIRAK"), CatItem("야식","LATE_NIGHT"),
        CatItem("찜/탕","JJIM_TANG")
    )
    private val compItems = listOf(
        CatItem("혼밥","ALONE"), CatItem("친구","FRIEND"),
        CatItem("연인","COUPLE"), CatItem("가족","FAMILY"),
        CatItem("단체","GROUP"), CatItem("반려동물 동반","WITH_PET"),
        CatItem("동호회","ALUMNI")
    )


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_category_select, null, false)


        val cgRegions = v.findViewById<ChipGroup>(R.id.chipGroupRegions)
        val PlusRegionButton = v.findViewById<View>(R.id.PlusRegionButton)
        //복원용
        selectedRegionCodes = savedInstanceState?.getStringArrayList(STATE_REGIONS)
            ?: selectedRegionCodes

        val cgMood = v.findViewById<ChipGroup>(R.id.chipGroupMood)
        val cgFood = v.findViewById<ChipGroup>(R.id.chipGroupFood)
        val cgComp = v.findViewById<ChipGroup>(R.id.chipGroupCompanion)

        val preMoods = requireArguments().getStringArrayList(KEY_MOODS) ?: arrayListOf()
        val preFoods = requireArguments().getStringArrayList(KEY_FOODS) ?: arrayListOf()
        val preComps = requireArguments().getStringArrayList(KEY_COMPANIONS) ?: arrayListOf()

        createChips(cgMood, moodItems, preMoods.toSet())
        createChips(cgFood, foodItems, preFoods.toSet())
        createChips(cgComp, compItems, preComps.toSet())

        // 지역 칩 미리보기(라벨은 코드 -> "서울 강남구" 로 변환)
        renderRegionChips(cgRegions, selectedRegionCodes)

        // 지역 선택 다이얼로그 결과 수신(이 다이얼로그의 childFM에서 받음)
        childFragmentManager.setFragmentResultListener(
            RegionSelectDialog.Companion.REQUEST_KEY, this
        ) { _, b ->
            val codes = b.getStringArrayList(RegionSelectDialog.Companion.KEY_SELECTED_CODES) ?: arrayListOf()
            selectedRegionCodes = ArrayList(codes) // 덮어쓰기(최신 선택 유지)
            Log.d(TAG, "Region result received: ${selectedRegionCodes.size} items -> $selectedRegionCodes")
            renderRegionChips(cgRegions, selectedRegionCodes)
        }

        // 지역 추가 버튼 → RegionSelectDialog 띄우기(미리 선택값 넘김)
        PlusRegionButton.setOnClickListener {
            RegionSelectDialog.Companion.newInstance(ArrayList(selectedRegionCodes))
                .show(childFragmentManager, "RegionSelectDialog")
        }

        v.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { dismiss() }
        v.findViewById<View>(R.id.btnDone).setOnClickListener {
            val labelsM = getCheckedLabels(cgMood)
            val labelsF = getCheckedLabels(cgFood)
            val labelsC = getCheckedLabels(cgComp)

            // ⬇️ 서버 전송 포맷: "moods:CODE" / "foods:CODE" / "companions:CODE"
            val payload = arrayListOf<String>().apply {
                addAll(getCheckedCodes(cgMood).map { "moods:$it" })
                addAll(getCheckedCodes(cgFood).map { "foods:$it" })
                addAll(getCheckedCodes(cgComp).map { "companions:$it" })
                addAll(selectedRegionCodes.map { "regions:$it" })
            }
            Log.d(TAG, "setFragmentResult OK moods=${labelsM.size}, foods=${labelsF.size}, comps=${labelsC.size}, regions=${selectedRegionCodes.size}, payload=${payload.size}")

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_MOODS to labelsM,          // UI 표시용 라벨
                    KEY_FOODS to labelsF,
                    KEY_COMPANIONS to labelsC,
                    KEY_PAYLOAD to payload         // 서버 전송용 합본
                )
            )
            dismiss()
        }


        return MaterialAlertDialogBuilder(requireContext(), R.style.App_MdcAlertDialog)
            .setView(v)
            .create()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_REGIONS, selectedRegionCodes)
    }



    private fun createChips(group: ChipGroup, items: List<CatItem>, preselectedLabels: Set<String>) {
        group.removeAllViews()
        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = item.label                 // 라벨 표시
                tag = item.code                   // 코드 저장(서버용)
                isCheckable = true
                isCheckedIconVisible = false
                isCloseIconVisible = false

                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                isChecked = preselectedLabels.contains(item.label)
            }
            group.addView(chip)
        }
    }

    private fun getCheckedLabels(group: ChipGroup): ArrayList<String> =
        ArrayList((0 until group.childCount)
            .mapNotNull { group.getChildAt(it) as? Chip }
            .filter { it.isChecked }
            .map { it.text.toString() })

    private fun getCheckedCodes(group: ChipGroup): ArrayList<String> =
        ArrayList((0 until group.childCount)
            .mapNotNull { group.getChildAt(it) as? Chip }
            .filter { it.isChecked }
            .map { it.tag as String })


    private fun renderRegionChips(cg: ChipGroup, codes: List<String>) {
        cg.removeAllViews()
        codes.forEach { code ->
            val label = code.replace("|", " ")
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                isCloseIconVisible = true   // X 버튼으로 선택 해제 가능
                // 스타일(기존 칩과 동일한 selector 사용)
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                setOnCloseIconClickListener {
                    // 개별 삭제
                    selectedRegionCodes.remove(code)
                    renderRegionChips(cg, selectedRegionCodes)
                }
            }
            cg.addView(chip)
        }
    }

    private fun renderRegionPreviewChips(
        group: ChipGroup,
        codes: Collection<String>
    ) {
        group.removeAllViews()
        if (codes.isEmpty()) return

        codes.forEach { code ->
            val label = code.substringAfter("|") // "서울|강남구" → "강남구"만 표시
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                includeFontPadding = false
                setEnsureMinTouchTargetSize(false)
                chipMinHeight = resources.displayMetrics.density * 28f
                setPadding(
                    (10 * resources.displayMetrics.density).toInt(),
                    0,
                    (10 * resources.displayMetrics.density).toInt(),
                    0
                )
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(resources.getColorStateList(R.color.selector_chip_text, null))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            group.addView(chip)
        }
    }
}