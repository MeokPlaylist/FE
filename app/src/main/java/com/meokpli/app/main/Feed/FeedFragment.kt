package com.meokpli.app.main.Feed

import SelectedPhotosAdapter
import com.meokpli.app.main.Roadmap.SaveRoadMapPlaceRequest
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.databinding.FragmentFeedBinding
import com.meokpli.app.gallery.GalleryBottomSheet
import com.meokpli.app.main.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Collections
import androidx.navigation.fragment.findNavController
import com.meokpli.app.main.Home.CategoryLabels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val TAG = "FeedFragment"

    // --- UI refs ---
    private lateinit var cameraBtn: View
    private lateinit var backBtn: View
    private lateinit var uploadBtn: View
    private lateinit var rvPhotos: RecyclerView
    private lateinit var photosAdapter: SelectedPhotosAdapter
    private lateinit var feedApi: MainApi

    // --- state ---
    private val selectedUris = mutableListOf<Uri>() // 사진 목록
    private var sel = SelectedCategories(emptyList(), emptyList(), emptyList())
    private var selectedPayload: MutableList<String> = mutableListOf()

    private var hashtagWatcher: TextWatcher? = null
    private val HASHTAG_COLOR = Color.parseColor("#FF0000")
    private val STATE_CONTENT = "state_feed_content"

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    // --- Presigned 업로드용 클라/함수 ---
    private val uploadClient by lazy { OkHttpClient.Builder().build() }

    private suspend fun readBytesFromUri(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        requireContext().contentResolver.openInputStream(uri).use { ins ->
            ins?.readBytes() ?: ByteArray(0)
        }
    }

    private suspend fun putToPresigned(url: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .put(bytes.toRequestBody("image/jpeg".toMediaType()))
            .header("Content-Type", "image/jpeg")
            .build()
        uploadClient.newCall(req).execute().use { resp ->
            val ok = resp.isSuccessful
            Log.i(TAG, "PUT presigned -> ${resp.code} (${bytes.size} bytes)")
            ok
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureMediaPerms()
        feedApi = Network.feedApi(requireContext())

        backBtn = view.findViewById(R.id.btnBack)
        cameraBtn = view.findViewById(R.id.btnCamera)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        uploadBtn = view.findViewById(R.id.btnUpload)

        // 내용 복원
        savedInstanceState?.getString(STATE_CONTENT)?.let { restored ->
            binding.etContent.setText(restored)
            binding.etContent.setSelection(restored.length)
        }

        // RecyclerView 설정
        rvPhotos.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        setupRecycler(view)
        setupHashtagWatcher()
        setupGalleryListener(view)

        // ✅ 카테고리 선택 결과 리스너
        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            val payload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD) ?: arrayListOf()
            val merged = (selectedPayload + payload).distinct()
            selectedPayload.clear()
            selectedPayload.addAll(merged)

            val moods = selectedPayload.filter { it.startsWith("moods:") }.map { it.removePrefix("moods:") }
            val foods = selectedPayload.filter { it.startsWith("foods:") }.map { it.removePrefix("foods:") }
            val companions = selectedPayload.filter { it.startsWith("companions:") }.map { it.removePrefix("companions:") }

            sel = SelectedCategories(moods, foods, companions)
            renderPreviewChips(view, selectedPayload)
        }
        view.setOnTouchListener { v, event ->
            hideKeyboard(v)
            false
        }
        // ✅ 카테고리 추가 버튼
        view.findViewById<TextView>(R.id.btnCategoryAdd)?.setOnClickListener {
            val moods = selectedPayload.filter { it.startsWith("moods:") }.map { it.removePrefix("moods:") }
            val foods = selectedPayload.filter { it.startsWith("foods:") }.map { it.removePrefix("foods:") }
            val companions = selectedPayload.filter { it.startsWith("companions:") }.map { it.removePrefix("companions:") }
            val regions = selectedPayload.filter { it.startsWith("regions:") }.map { it.removePrefix("regions:") }

            val reverseMoodMap = mapOf(
                "TRADITIONAL" to "전통적인", "UNIQUE" to "이색적인", "EMOTIONAL" to "감성적인",
                "HEALING" to "힐링되는", "GOODVIEW" to "뷰 맛집", "ACTIVITY" to "활기찬", "LOCAL" to "로컬"
            )
            val reverseFoodMap = mapOf(
                "BUNSIK" to "분식","CAFE_DESSERT" to "카페/디저트","CHICKEN" to "치킨","CHINESE" to "중식",
                "KOREAN" to "한식","PORK_SASHIMI" to "돈까스/회","FASTFOOD" to "패스트푸드","JOKBAL_BOSSAM" to "족발/보쌈",
                "PIZZA" to "피자","WESTERN" to "양식","MEAT" to "고기","ASIAN" to "아시안","DOSIRAK" to "도시락",
                "LATE_NIGHT" to "야식","JJIM_TANG" to "찜/탕"
            )
            val reverseCompanionMap = mapOf(
                "ALONE" to "혼밥","FRIEND" to "친구","COUPLE" to "연인","FAMILY" to "가족",
                "GROUP" to "단체","WITH_PET" to "반려동물 동반","ALUMNI" to "동호회"
            )

            val moodLabels = moods.mapNotNull { reverseMoodMap[it] }
            val foodLabels = foods.mapNotNull { reverseFoodMap[it] }
            val compLabels = companions.mapNotNull { reverseCompanionMap[it] }

            val dialog = CategorySelectDialog.newInstance(
                ArrayList(moodLabels),
                ArrayList(foodLabels),
                ArrayList(compLabels)
            )
            val mergedArgs = (dialog.arguments ?: Bundle()).apply {
                putStringArrayList(
                    "state_regions",
                    ArrayList(regions.map { CategoryLabels.regionToKorean(it) })
                )
            }
            dialog.arguments = mergedArgs
            dialog.show(parentFragmentManager, "CategorySelectDialog")
        }

        uploadBtn.setOnClickListener { doUpload(view) }
        cameraBtn.setOnClickListener { openGalleryBottomSheet() }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }
    }

    // ---------- RecyclerView ----------
    private fun setupRecycler(view: View) {
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {
            override fun isLongPressDragEnabled() = false
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                Collections.swap(selectedUris, from, to)
                photosAdapter.submitList(selectedUris.toList())
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        val touchHelper = ItemTouchHelper(touchHelperCallback)

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
    }

    // ---------- 해시태그 색칠 ----------
    private fun setupHashtagWatcher() {
        hashtagWatcher = object : TextWatcher {
            private var running = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (running || s == null) return


                running = true
                try { highlightHashtags(s) } finally { running = false }
            }
        }
        binding.etContent.addTextChangedListener(hashtagWatcher)
    }

    // ---------- 갤러리 리스너 ----------
    private fun setupGalleryListener(view: View) {
        parentFragmentManager.setFragmentResultListener(GalleryBottomSheet.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            val uris = bundle.getParcelableArrayList<Uri>(GalleryBottomSheet.RESULT_URIS) ?: arrayListOf()
            selectedUris.clear()
            selectedUris.addAll(uris)
            photosAdapter.submitList(selectedUris.toList())
            view.findViewById<View>(R.id.emptyPhotoBox)?.visibility =
                if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
            if (selectedUris.isNotEmpty()) rvPhotos.scrollToPosition(0)
        }
    }

    // ---------- 칩 렌더링 ----------
    private fun renderPreviewChips(root: View, payload: List<String>) {
        val cg = root.findViewById<ViewGroup>(R.id.chipGroupCategoryPreview)
        cg.removeAllViews()

        val reverseMoodMap = mapOf(
            "TRADITIONAL" to "전통적인", "UNIQUE" to "이색적인", "EMOTIONAL" to "감성적인",
            "HEALING" to "힐링되는", "GOODVIEW" to "뷰 맛집", "ACTIVITY" to "활기찬", "LOCAL" to "로컬"
        )
        val reverseFoodMap = mapOf(
            "BUNSIK" to "분식","CAFE_DESSERT" to "카페/디저트","CHICKEN" to "치킨","CHINESE" to "중식",
            "KOREAN" to "한식","PORK_SASHIMI" to "돈까스/회","FASTFOOD" to "패스트푸드","JOKBAL_BOSSAM" to "족발/보쌈",
            "PIZZA" to "피자","WESTERN" to "양식","MEAT" to "고기","ASIAN" to "아시안","DOSIRAK" to "도시락",
            "LATE_NIGHT" to "야식","JJIM_TANG" to "찜/탕"
        )
        val reverseCompanionMap = mapOf(
            "ALONE" to "혼밥","FRIEND" to "친구","COUPLE" to "연인","FAMILY" to "가족",
            "GROUP" to "단체","WITH_PET" to "반려동물 동반","ALUMNI" to "동호회"
        )

        payload.forEach { raw ->
            val label = when {
                // ✅ 지역명 변환: 영어 → 한글 + 콜론(:) → 공백
                raw.startsWith("regions:") -> {
                    val region = raw.removePrefix("regions:")
                    CategoryLabels.regionToKorean(region).replace(":", " ")
                }
                raw.startsWith("moods:") -> reverseMoodMap[raw.removePrefix("moods:")] ?: raw
                raw.startsWith("foods:") -> reverseFoodMap[raw.removePrefix("foods:")] ?: raw
                raw.startsWith("companions:") -> reverseCompanionMap[raw.removePrefix("companions:")] ?: raw
                else -> raw
            }

            val chipView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_chip, cg, false)

            chipView.findViewById<TextView>(R.id.chipText).text = label

            chipView.findViewById<ImageView>(R.id.chipClose).setOnClickListener {
                selectedPayload.remove(raw)
                val moods = selectedPayload.filter { it.startsWith("moods:") }.map { it.removePrefix("moods:") }
                val foods = selectedPayload.filter { it.startsWith("foods:") }.map { it.removePrefix("foods:") }
                val companions = selectedPayload.filter { it.startsWith("companions:") }.map { it.removePrefix("companions:") }
                sel = SelectedCategories(moods, foods, companions)
                renderPreviewChips(root, selectedPayload)
            }

            cg.addView(chipView)
        }
    }

    // ---------- 권한 ----------
    private fun ensureMediaPerms() {
        val perms = when {
            Build.VERSION.SDK_INT >= 33 ->
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            Build.VERSION.SDK_INT >= 29 ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            else ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { }.launch(perms)
    }

    private fun highlightHashtags(editable: Editable) {
        val old = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
        for (span in old) if (span.foregroundColor == HASHTAG_COLOR) editable.removeSpan(span)
        val regex = Regex("""#([^\s#]+)""")
        regex.findAll(editable.toString()).forEach { m ->
            editable.setSpan(ForegroundColorSpan(HASHTAG_COLOR), m.range.first, m.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // ---------- 업로드 ----------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun doUpload(view: View) {
        if (selectedUris.isEmpty()) {
            Toast.makeText(requireContext(), "사진을 최소 1장 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val contentText = binding.etContent.text?.toString()?.trim().orEmpty()
        val contentNullable = contentText.takeIf { it.isNotBlank() }

        val categoryReq = CategoryRequest(
            mood = sel.moods.takeIf { it.isNotEmpty() }.orEmpty(),
            food = sel.foods.takeIf { it.isNotEmpty() }.orEmpty(),
            companion = sel.companions.takeIf { it.isNotEmpty() }.orEmpty(),
        )
        val regionStrings = selectedPayload.filter { it.startsWith("regions:") }.map { it.removePrefix("regions:") }.takeIf { it.isNotEmpty() }
        val hashtags = extractHashtags(contentText).takeIf { it.isNotEmpty() }

        val metas: List<PhotoMeta> = selectedUris.map { extractPhotoMeta(requireContext(), it) }
        val photos: List<ClientPhoto> = metas.mapIndexed { idx, m ->
            ClientPhoto(
                fileName = m.fileName,
                latitude = m.latitude,
                longitude = m.longitude,
                dayAndTime = m.dateTimeOriginalIso ?: LocalDateTime.now().withSecond(0).withNano(0).toString(),
                sequence = idx + 1
            )
        }

        Log.i(TAG, "⏫ createFeed 요청 준비: content='${contentNullable?.take(40)}', tags=${hashtags?.size ?: 0}, " +
                "cat=${(sel.moods + sel.foods + sel.companions).size}, regions=${regionStrings?.size ?: 0}, photos=${photos.size}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1) presigned URL & feedId 받기
                val body = FeedRequestBuilder.buildBody(
                    content = contentNullable,
                    hashTags = hashtags,
                    categories = categoryReq,
                    regions = regionStrings,
                    photos = photos
                )
                val resp = feedApi.createFeed(body)
                if (!resp.isSuccessful) {
                    Toast.makeText(requireContext(), "실패: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val createRes = resp.body()
                val feedId = createRes?.feedId
                val uploadUrls = createRes?.presignedPutUrls.orEmpty()

                if (feedId == null) {
                    Toast.makeText(requireContext(), "feedId를 수신하지 못했습니다.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (uploadUrls.size != selectedUris.size) {
                    Toast.makeText(requireContext(), "업로드 개수 불일치 (${uploadUrls.size} vs ${selectedUris.size})", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 2) presigned URL로 사진 PUT 업로드
                for (i in uploadUrls.indices) {
                    val bytes = readBytesFromUri(selectedUris[i])
                    if (bytes.isEmpty()) {
                        Toast.makeText(requireContext(), "사진 읽기 실패: ${i + 1}/${uploadUrls.size}", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val ok = putToPresigned(uploadUrls[i], bytes)
                    if (!ok) {
                        Toast.makeText(requireContext(), "사진 업로드 실패: ${i + 1}/${uploadUrls.size}", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                Toast.makeText(requireContext(), "업로드 완료", Toast.LENGTH_SHORT).show()
                val args = Bundle().apply { putLong("feedId", feedId) }
                findNavController().navigate(R.id.action_feed_to_roadmapEdit, args)

            } catch (e: Exception) {
                Log.e(TAG, "upload error", e)
                Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- 유틸 ----------
    private fun openGalleryBottomSheet() {
        (parentFragmentManager.findFragmentByTag("gallery") as? GalleryBottomSheet)?.let {
            if (it.dialog?.isShowing == true) return
            it.dismissAllowingStateLoss()
        }
        GalleryBottomSheet.newInstance(ArrayList(selectedUris)).show(parentFragmentManager, "gallery")
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    private fun extractHashtags(text: String): List<String> =
        Regex("""#([^\s#]+)""").findAll(text).map { it.groupValues[1] }.toList()

    data class SelectedCategories(val moods: List<String>, val foods: List<String>, val companions: List<String>)
}
