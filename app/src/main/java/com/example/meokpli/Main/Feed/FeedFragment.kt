package com.example.meokpli.Main.Feed

import SelectedPhotosAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.CategoryRequest
import com.example.meokpli.Main.CategorySelectDialog
import com.example.meokpli.Main.ClientPhoto
import com.example.meokpli.Main.FeedRequestBuilder
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.Main.MainApi
import com.example.meokpli.Main.Feed.PresignedUploader
import com.example.meokpli.R
import com.example.meokpli.gallery.GalleryBottomSheet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Collections

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val TAG = "FeedFragment"

    // --- UI refs ---
    private lateinit var cameraBtn: View
    private lateinit var backBtn: View

    private lateinit var uploadBtn : View
    private lateinit var rvPhotos: RecyclerView
    private lateinit var photosAdapter: SelectedPhotosAdapter
    private lateinit var feedApi: MainApi

    // --- state ---
    private val selectedUris = mutableListOf<Uri>()             // 사진 선택 목록
    private var sel = SelectedCategories(emptyList(), emptyList(), emptyList())
    private var selectedPayload: ArrayList<String> = arrayListOf() // 서버 전송용(moods/foods/companions/regions)
    private lateinit var etContent: EditText
    private var hashtagWatcher: TextWatcher? = null
    private val HASHTAG_COLOR = Color.parseColor("#FF0000")
    private val STATE_CONTENT = "state_feed_content"
    // dp helper
    private fun View.dp(v: Float) = v * resources.displayMetrics.density

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_feed, container, false)


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ensureMediaPerms()

        feedApi = Network.feedApi(requireContext())
        // 버튼/리사이클러뷰 바인딩
        backBtn = view.findViewById(R.id.btnBack)
        cameraBtn = view.findViewById(R.id.btnCamera)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        uploadBtn = view.findViewById(R.id.btnUpload)
        etContent = view.findViewById(R.id.etContent)

        // 내용 복원
        savedInstanceState?.getString(STATE_CONTENT)?.let { restored ->
            etContent.setText(restored)
            etContent.setSelection(restored.length)
        }

        // 리사이클러뷰
        rvPhotos.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvPhotos.setHasFixedSize(true)

        // 실시간 색칠 TextWatcher
        hashtagWatcher = object : TextWatcher {
            private var running = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (running || s == null) return
                running = true
                try {
                    highlightHashtags(s)   // 아래 함수
                } finally {
                    running = false
                }
            }
        }
        etContent.addTextChangedListener(hashtagWatcher)

        // 1) ItemTouchHelper: 롱프레스 드래그 비활성화(아이템 내 핸들에서만)
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                Collections.swap(selectedUris, from, to)
                photosAdapter.submitList(selectedUris.toList())
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* no-op */ }
        }
        val touchHelper = ItemTouchHelper(touchHelperCallback)

        // 2) 어댑터: touchHelper 주입 + 클릭/삭제 콜백
        photosAdapter = SelectedPhotosAdapter(
            itemTouchHelper = touchHelper,
            onItemClick = { _, _ -> openGalleryBottomSheet() },
            onRemoveClick = { pos, _ ->
                if (pos in selectedUris.indices) {
                    selectedUris.removeAt(pos)
                    photosAdapter.submitList(selectedUris.toList())
                    view.findViewById<View>(R.id.emptyPhotoBox)?.visibility =
                        if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        )
        rvPhotos.adapter = photosAdapter
        touchHelper.attachToRecyclerView(rvPhotos)

        // 3) 갤러리 바텀시트 결과 수신 (이미지 선택)
        parentFragmentManager.setFragmentResultListener(
            GalleryBottomSheet.Companion.RESULT_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val uris = bundle.getParcelableArrayList<Uri>(GalleryBottomSheet.Companion.RESULT_URIS) ?: arrayListOf()
            selectedUris.clear()
            selectedUris.addAll(uris)                           // 순서 보존
            photosAdapter.submitList(selectedUris.toList())

            view.findViewById<View>(R.id.emptyPhotoBox)?.visibility =
                if (selectedUris.isEmpty()) View.VISIBLE else View.GONE

            if (selectedUris.isNotEmpty()) rvPhotos.scrollToPosition(0)
            Toast.makeText(requireContext(), "선택: ${uris.size}장", Toast.LENGTH_SHORT).show()
        }

        // 4) 카테고리/지역 다이얼로그 결과 수신
        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.Companion.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            Log.d(TAG, "Result received: keys=${b.keySet()}")

            selectedPayload = b.getStringArrayList(CategorySelectDialog.Companion.KEY_PAYLOAD) ?: arrayListOf()
            if (selectedPayload.isEmpty()) {
                Log.w(TAG, "Received bundle but payload is null/empty")
                Toast.makeText(requireContext(), "선택된 카테고리가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Payload size=${selectedPayload.size}, payload=$selectedPayload")
            }

            sel = SelectedCategories(
                b.getStringArrayList(CategorySelectDialog.Companion.KEY_MOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.Companion.KEY_FOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.Companion.KEY_COMPANIONS) ?: arrayListOf()
            )
            Log.d(TAG, "Labels m=${sel.moods.size}, f=${sel.foods.size}, c=${sel.companions.size}")

            renderPreviewChips(view, sel) // 지역 라벨은 payload에서 파싱
        }

        // 카테고리 다이얼로그 열기 (기존 선택값 전달)
        view.findViewById<TextView>(R.id.btnCategoryAdd)?.setOnClickListener {
            CategorySelectDialog.Companion.newInstance(
                ArrayList(sel.moods), ArrayList(sel.foods), ArrayList(sel.companions)
            ).show(parentFragmentManager, "CategorySelectDialog")
        }

        // 5) 업로드 버튼
        uploadBtn.setOnClickListener {
            val contentText = etContent.text?.toString()?.trim().orEmpty()
            val contentNullable = contentText.takeIf { it.isNotBlank() } // 비었으면 null

            val categoryReq = CategoryRequest(
                mood = sel.moods.takeIf { it.isNotEmpty() }.orEmpty(),           // List<String>
                food = sel.foods.takeIf { it.isNotEmpty() }.orEmpty(),
                companion = sel.companions.takeIf { it.isNotEmpty() }.orEmpty(),
            )

            val regionStrings: List<String>? =
                parseRegionLabelsFromPayload(selectedPayload).takeIf { it.isNotEmpty() } // 예: ["서울:구로구", "부산:해운대구"]


            val hashtags: List<String>? =
                extractHashtags(contentText).takeIf { it.isNotEmpty() } // List<String>?


            val metas: List<PhotoMeta> = selectedUris.map { extractPhotoMeta(requireContext(), it) }

            val photos: List<ClientPhoto> = metas.mapIndexed { idx, m ->
                ClientPhoto(
                    fileName = m.fileName,                            // 서버가 인식할 파일명/키
                    latitude = m.latitude,
                    longitude = m.longitude,
                    dayAndTime = m.dateTimeOriginalIso
                        ?: LocalDateTime.now().withSecond(0).withNano(0).toString(),
                    sequence = idx + 1
                )
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val body = FeedRequestBuilder.buildBody(
                        content = contentNullable,
                        hashTags = hashtags,
                        categories = categoryReq,
                        regions = regionStrings,
                        photos = photos
                    )
                    Log.d("request",body.toString())
                    val resp = feedApi.createFeed(body)
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "업로드 성공", Toast.LENGTH_SHORT).show()
                        // 업로드 API 호출 후:
                        val uploadUrls: List<String> = resp.body()?.presignedPutUrls.orEmpty()  // 서버 키 이름에 맞게 조정
                        val localUris: List<Uri> = selectedUris.toList()            // 네가 고른 사진들
                        Log.d("response",uploadUrls.toString())
                        // 둘 길이 검증
                        if (uploadUrls.size != localUris.size) {
                            Log.w(TAG, "서버 URL 개수(${uploadUrls.size}) != 선택한 사진 개수(${localUris.size})")
                        }

                        // 실제 업로드
                        val results = PresignedUploader.uploadAll(
                            context = requireContext(),
                            uris = localUris,
                            urls = uploadUrls,
                            method = "PUT",                 // 서버 요구에 맞게
                            headers = emptyMap(),           // 필요시 Content-MD5 등 추가
                            onProgress = { index, sent, total ->
                                // 진행률 UI 갱신 (옵션)
                                // val pct = if (total > 0) (sent * 100 / total) else -1
                            }
                        )

                        // 결과 확인
                        if (results.all { it }) {
                            Toast.makeText(requireContext(), "원본 업로드 완료", Toast.LENGTH_SHORT).show()
                            (requireActivity() as? MainActivity)?.handleSystemBack()
                        } else {
                            val failed = results.withIndex().filter { !it.value }.map { it.index }
                            Toast.makeText(requireContext(), "업로드 실패 인덱스: $failed", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(requireContext(), "실패: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "upload error", e)
                    Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // 6) 버튼 리스너
        cameraBtn.setOnClickListener { openGalleryBottomSheet() }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }
    }

    // --- GalleryBottomSheet 열기(중복 방지 포함) ---
    private fun openGalleryBottomSheet() {
        // 이미 떠있으면 중복 방지
        (parentFragmentManager.findFragmentByTag("gallery") as? GalleryBottomSheet)?.let {
            if (it.dialog?.isShowing == true) return
            it.dismissAllowingStateLoss()
        }
        // 현재 선택을 전달하여 초기 체크 & 순서 유지
        GalleryBottomSheet.Companion.newInstance(ArrayList(selectedUris))
            .show(parentFragmentManager, "gallery")
    }

    /** payload에서 지역 라벨(시/구/군)만 추출 — 구분자는 ':'만 사용 */
    private fun parseRegionLabelsFromPayload(payload: List<String>): List<String> =
        payload.asSequence()
            .map { it.trim() }
            .filter { it.startsWith("regions:") }
            .map { it.removePrefix("regions:").trim() }   // "서울:구로구" 또는 "서울"
            .map { body ->
                val idx = body.indexOf(':')               // 첫 ':' 기준 분리
                if (idx >= 0) {
                    val sido = body.substring(0, idx).trim()
                    val sigungu = body.substring(idx + 1).trim()
                    if (sigungu.isNotEmpty()) sigungu else sido
                } else {
                    body                                   // 시군구가 없으면 시도 사용
                }
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

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
            val label = raw.trim()
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                includeFontPadding = false
                setEnsureMinTouchTargetSize(false)
                chipMinHeight = root.dp(28f)

                chipStartPadding = root.dp(8f)
                chipEndPadding = root.dp(2f)
                textStartPadding = 0f
                textEndPadding = 0f
                iconStartPadding = 0f
                iconEndPadding = 0f

                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFE7E7"))
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#C64132"))
                chipStrokeWidth = root.dp(1f)
                setTextColor(Color.parseColor("#C64132"))

                textSize = 12f
            }
            cg.addView(chip)
        }

        cg.post {
            Log.d(TAG, "renderPreviewChips: childCount=${cg.childCount}, size=${labels.size}")
        }
    }
    //GPS받는 권한
    private fun ensureMediaPerms() {
        val perms = when {
            Build.VERSION.SDK_INT >= 33 ->
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            Build.VERSION.SDK_INT >= 29 ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            else ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { /* 결과 체크 */ }
            .launch(perms)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CONTENT, getFeedContent())  // ← (5) 추가
    }

    /** EditText의 현재 내용을 깔끔하게 가져오기 */
    private fun getFeedContent(): String =
        etContent.text?.toString()?.trim().orEmpty()

    /** 실시간 색상 하이라이트: #부터 다음 공백 전까지 빨간색 */
    private fun highlightHashtags(editable: Editable) {
        // 1) 기존 우리가 칠했던 빨간 span 제거
        val old = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
        for (span in old) {
            // 우리가 넣은 빨간색만 지우기
            @Suppress("DEPRECATION")
            if (span.foregroundColor == HASHTAG_COLOR) {
                editable.removeSpan(span)
            }
        }
        // 2) 새로 스캔해서 다시 칠하기
        // 규칙: '#' 다음으로 공백/개행/탭 전까지(또는 문자열 끝까지)
        val regex = Regex("""#([^\s#]+)""")  // 공백 나오기 전까지 연속 문자, 중복 '#' 구분
        val text = editable.toString()
        regex.findAll(text).forEach { m ->
            val start = m.range.first
            val end = m.range.last + 1
            editable.setSpan(
                ForegroundColorSpan(HASHTAG_COLOR),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /** 업로드용 해시태그 추출: '#' 제거해서 리스트로 반환 */
    private fun extractHashtags(text: String): List<String> =
        Regex("""#([^\s#]+)""")
            .findAll(text)
            .map { it.groupValues[1] }   // "여행", "맛집" 처럼 # 제외
            .toList()

    // 미리보기/전송용 DTO
    data class SelectedCategories(
        val moods: List<String>,
        val foods: List<String>,
        val companions: List<String>
    )
}