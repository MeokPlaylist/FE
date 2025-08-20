package com.example.meokpli.Main

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.meokpli.R
import com.example.meokpli.gallery.GalleryActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    // 갤러리 결과
    private val launchGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK && r.data != null) {
            val uris = r.data!!.getParcelableArrayListExtra<Uri>(GalleryActivity.EXTRA_RESULT_URIS)
                ?: arrayListOf()
            Toast.makeText(requireContext(), "선택: ${uris.size}장", Toast.LENGTH_SHORT).show()
        }
    }

    // 카테고리/지역 선택값 보관
    private var sel = SelectedCategories(emptyList(), emptyList(), emptyList())
    private var selectedPayload: ArrayList<String> = arrayListOf() // 서버 전송용 전부(moods/foods/companions/regions)

    // dp 변환
    private fun View.dp(v: Float) = v * resources.displayMetrics.density

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 다이얼로그 결과 수신
        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            Log.d(TAG, "Result received: keys=${b.keySet()}")

            // payload는 null일 수 있음 → 안전 처리
            selectedPayload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD) ?: arrayListOf()
            if (selectedPayload.isEmpty()) {
                Log.w(TAG, "Received bundle but payload is null/empty")
                Toast.makeText(requireContext(), "선택된 카테고리가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Payload size=${selectedPayload.size}, payload=$selectedPayload")
            }

            sel = SelectedCategories(
                b.getStringArrayList(CategorySelectDialog.KEY_MOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.KEY_FOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.KEY_COMPANIONS) ?: arrayListOf()
            )
            Log.d(TAG, "Labels m=${sel.moods.size}, f=${sel.foods.size}, c=${sel.companions.size}")



            // 미리보기 갱신 (지역은 selectedPayload에서 파싱)
            renderPreviewChips(view, sel)
        }

        // 툴바 메뉴(업로드)
        view.findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload -> {
                        openGallery()
                        true
                    }
                    else -> false
                }
            }
            setNavigationOnClickListener {
                Toast.makeText(requireContext(), "뒤로가기", Toast.LENGTH_SHORT).show()
            }
        }

        // 카테고리 다이얼로그 열기 (기존 선택값 전달)
        view.findViewById<TextView>(R.id.btnCategoryAdd)?.setOnClickListener {
            CategorySelectDialog.newInstance(
                ArrayList(sel.moods), ArrayList(sel.foods), ArrayList(sel.companions)
            ).show(parentFragmentManager, "CategorySelectDialog")
        }
    }

    private fun openGallery() {
        val intent = Intent(requireContext(), GalleryActivity::class.java)
        launchGallery.launch(intent)
    }

    /** payload에서 지역 라벨(시/구/군)만 추출 */
    private fun parseRegionLabelsFromPayload(payload: List<String>): List<String> =
        payload.filter { it.startsWith("regions:") }
            .map { it.removePrefix("regions:") }   // "서울|구로구"
            .map { it.substringAfter("|") }        // "구로구"

    /** 상단 미리보기 칩 생성 (카테고리 + 지역) */
    private fun renderPreviewChips(root: View, s: SelectedCategories) {
        val cg = root.findViewById<ChipGroup>(R.id.chipGroupCategoryPreview)
        cg.removeAllViews()

        val regionLabels = parseRegionLabelsFromPayload(selectedPayload)
        val labels = s.moods + s.foods + s.companions + regionLabels

        if (labels.isEmpty()) {
            val empty = Chip(requireContext()).apply {
                text = "선택 없음"
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#EEEEEE"))
                setTextColor(Color.parseColor("#888888"))
                chipStrokeWidth = 0f
            }
            cg.addView(empty)
            return
        }

        labels.forEach { raw ->
            val label = raw.trim() // 혹시 모를 공백 방지
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                includeFontPadding = false
                setEnsureMinTouchTargetSize(false)
                chipMinHeight = root.dp(28f)

                // 여백 최소화 (글자 뒤 공백 줄이기)
                chipStartPadding = root.dp(8f)
                chipEndPadding = root.dp(2f)
                textStartPadding = 0f
                textEndPadding = 0f
                iconStartPadding = 0f
                iconEndPadding = 0f

                // 스타일
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFE7E7"))
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#C64132"))
                chipStrokeWidth = root.dp(1f)
                setTextColor(Color.parseColor("#C64132"))

                // 텍스트 크기
                textSize = 12f
            }
            cg.addView(chip)
        }

        cg.post {
            Log.d(TAG, "renderPreviewChips: childCount=${cg.childCount}, size=${labels.size}")
        }
    }

    private fun parseRegionsFromPayload(payload: List<String>): List<RegionReq> {
        return payload.asSequence()
            .filter { it.startsWith("regions:") }
            .map { it.removePrefix("regions:") }   // "서울|구로구" or "서울|"
            .map { body ->
                val sido = body.substringBefore("|")
                val sigg = body.substringAfter("|", missingDelimiterValue = "")
                    .ifBlank { null }              // 빈 문자열이면 null 처리
                RegionReq(sido = sido, sigungu = sigg)
            }
            .distinct()                             // 중복 제거(옵션)
            .toList()
    }

    // 미리보기용 dataset
    data class SelectedCategories(
        val moods: List<String>,
        val foods: List<String>,
        val companions: List<String>
    )
}
