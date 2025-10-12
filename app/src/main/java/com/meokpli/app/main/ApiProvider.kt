package com.meokpli.app.main

import TokenManager
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.meokpli.app.main.Feed.RegionSelectDialog
import com.meokpli.app.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meokpli.app.auth.AuthApi
import com.meokpli.app.auth.AuthInterceptor
import com.meokpli.app.auth.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import com.meokpli.app.main.Home.CategoryLabels
import android.graphics.Color

object ApiProvider {
    fun create(tokenManager: TokenManager, api: AuthApi): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(AuthInterceptor(tokenManager))   // 요청에 토큰 부착
            .authenticator(TokenAuthenticator(api, tokenManager)) // 401 시 refresh
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
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
            preRegions: ArrayList<String> = arrayListOf(),
        ) = CategorySelectDialog().apply {
            arguments = bundleOf(
                KEY_MOODS to preMoods,
                KEY_FOODS to preFoods,
                KEY_COMPANIONS to preComps,
                STATE_REGIONS to preRegions
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
        CatItem("분식","BUNSIK"), CatItem("카페/디저트","CAFE_DESSERT"),
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
        val preRegions = requireArguments().getStringArrayList(STATE_REGIONS) ?: arrayListOf()

        createChips(cgMood, moodItems, preMoods.toSet())
        createChips(cgFood, foodItems, preFoods.toSet())
        createChips(cgComp, compItems, preComps.toSet())

        // FeedFragment에서 전달한 지역 정보 복원
        if (preRegions.isNotEmpty()) {
            selectedRegionCodes = ArrayList(preRegions.map { CategoryLabels.regionToKorean(it) })
            renderRegionChips(cgRegions, selectedRegionCodes)
        }

        // 지역 칩 미리보기(라벨은 코드 -> "서울 강남구" 로 변환)
        renderRegionChips(cgRegions, selectedRegionCodes)

        // 지역 선택 다이얼로그 결과 수신(이 다이얼로그의 childFM에서 받음)
        parentFragmentManager.setFragmentResultListener(
            RegionSelectDialog.REQUEST_KEY, this
        ) { _, b ->
            val codes = b.getStringArrayList(RegionSelectDialog.KEY_SELECTED_CODES) ?: arrayListOf()
            Log.d("CategorySelectDialog", "✅ Region 결과 수신: $codes")

            // ✅ 중복 제거 (강남구 여러 번 추가 방지)
            val merged = (selectedRegionCodes + codes).distinct()
            selectedRegionCodes = ArrayList(merged)

            renderRegionChips(cgRegions, selectedRegionCodes)
        }


        // 지역 추가 버튼 → RegionSelectDialog 띄우기(미리 선택값 넘김)
        PlusRegionButton.setOnClickListener {
            RegionSelectDialog.Companion.newInstance(ArrayList(selectedRegionCodes))
                .show(parentFragmentManager, "RegionSelectDialog")
        }

        v.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { dismiss() }
        v.findViewById<View>(R.id.btnDone).setOnClickListener {
            val labelsM = getCheckedLabels(cgMood)
            val labelsF = getCheckedLabels(cgFood)
            val labelsC = getCheckedLabels(cgComp)

            val regionforServer = selectedRegionCodes.map { code ->
                val parts = code.split(":", limit = 2)
                val sidoKo = parts.getOrNull(0).orEmpty()
                val sggKo  = parts.getOrNull(1).orEmpty()
                CategoryLabels.regionToServer(sidoKo, sggKo)
            }

            // ⬇️ 서버 전송 포맷: "moods:CODE" / "foods:CODE" / "companions:CODE"
            val payload = arrayListOf<String>().apply {
                addAll(getCheckedCodes(cgMood).map { "moods:$it" })
                addAll(getCheckedCodes(cgFood).map { "foods:$it" })
                addAll(getCheckedCodes(cgComp).map { "companions:$it" })
                addAll(regionforServer.map { "regions:$it" })
            }
            Log.d("CategorySelectDialog", "✅ btnDone 클릭, 최종 payload = $payload")

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
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()



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
        if (codes.isEmpty()) {
            val empty = layoutInflater.inflate(R.layout.item_chip, cg, false)
            empty.findViewById<TextView>(R.id.chipText).apply {
                text = "선택 없음"
                setTextColor(Color.parseColor("#888888"))
            }
            empty.findViewById<ImageView>(R.id.chipClose).visibility = View.GONE
            cg.addView(empty)
            return
        }

        codes.forEach { code ->
            val parts = code.split(":", limit = 2)
            val sido = parts.getOrNull(0).orEmpty()
            val sgg  = parts.getOrNull(1).orEmpty()

            val v = layoutInflater.inflate(R.layout.item_chip, cg, false)
            val tv = v.findViewById<TextView>(R.id.chipText)
            val close = v.findViewById<ImageView>(R.id.chipClose)

            tv.text = "$sido $sgg"         // ✅ 한글 표시
            // 살짝 왼쪽으로 당기고 싶으면 start padding만 줄이세요
            tv.setPadding(dp(6), tv.paddingTop, tv.paddingRight, tv.paddingBottom)

            close.visibility = View.VISIBLE
            close.setOnClickListener {
                selectedRegionCodes.remove(code)
                renderRegionChips(cg, selectedRegionCodes)
            }
            cg.addView(v)
        }
    }
}