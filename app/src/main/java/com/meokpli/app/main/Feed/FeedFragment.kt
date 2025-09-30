package com.meokpli.app.Main.Feed

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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.databinding.FragmentFeedBinding
import com.meokpli.app.gallery.GalleryBottomSheet
import com.meokpli.app.main.CategoryRequest
import com.meokpli.app.main.CategorySelectDialog
import com.meokpli.app.main.ClientPhoto
import com.meokpli.app.main.Feed.PhotoMeta
import com.meokpli.app.main.Feed.PresignedUploader
import com.meokpli.app.main.Feed.extractPhotoMeta
import com.meokpli.app.main.FeedRequestBuilder
import com.meokpli.app.main.MainActivity
import com.meokpli.app.main.MainApi
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Collections

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
    private val selectedUris = mutableListOf<Uri>()             // 사진 선택 목록
    private var sel = SelectedCategories(emptyList(), emptyList(), emptyList())

    private var selectedPayload: MutableList<String> = mutableListOf()

    private var hashtagWatcher: TextWatcher? = null

    private val HASHTAG_COLOR = Color.parseColor("#FF0000")
    private val STATE_CONTENT = "state_feed_content"

    private fun View.dp(v: Float) = v * resources.displayMetrics.density
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 카테고리 결과 받기 (onCreate에서 등록)
        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, this
        ) { _, b ->
            selectedPayload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD) ?: arrayListOf()
            Log.d("FeedFragment", "✅ Category 결과 수신: $selectedPayload")
            view?.let { renderPreviewChips(it, selectedPayload) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // RecyclerView
        rvPhotos.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvPhotos.setHasFixedSize(true)

        // 해시태그 색칠
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

        // 어댑터 & TouchHelper
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

        // 갤러리 결과
        parentFragmentManager.setFragmentResultListener(
            GalleryBottomSheet.RESULT_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val uris = bundle.getParcelableArrayList<Uri>(GalleryBottomSheet.RESULT_URIS) ?: arrayListOf()
            selectedUris.clear()
            selectedUris.addAll(uris)
            photosAdapter.submitList(selectedUris.toList())
            view.findViewById<View>(R.id.emptyPhotoBox)?.visibility =
                if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
            if (selectedUris.isNotEmpty()) rvPhotos.scrollToPosition(0)
        }

        // 카테고리 버튼
        view.findViewById<TextView>(R.id.btnCategoryAdd)?.setOnClickListener {
            CategorySelectDialog.newInstance(
                ArrayList(sel.moods), ArrayList(sel.foods), ArrayList(sel.companions)
            ).show(parentFragmentManager, "CategorySelectDialog")
        }


        // 업로드 버튼
        uploadBtn.setOnClickListener { doUpload(view) }

        cameraBtn.setOnClickListener { openGalleryBottomSheet() }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }
    }

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
        val regionStrings = selectedPayload
            .filter { it.startsWith("regions:") }
            .map { it.removePrefix("regions:") }
            .takeIf { it.isNotEmpty() } // 서버 전송용은 ":" 유지
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = FeedRequestBuilder.buildBody(
                    content = contentNullable,
                    hashTags = hashtags,
                    categories = categoryReq,
                    regions = regionStrings, // ✅ 서버로는 ":" 포함
                    photos = photos
                )
                val resp = feedApi.createFeed(body)
                if (resp.isSuccessful) {
                    val uploadUrls = resp.body()?.presignedPutUrls.orEmpty()
                    val results = PresignedUploader.uploadAll(
                        context = requireContext(),
                        uris = selectedUris.toList(),
                        urls = uploadUrls
                    )
                    if (results.all { it }) {
                        Toast.makeText(requireContext(), "업로드 완료", Toast.LENGTH_SHORT).show()
                        (requireActivity() as? MainActivity)?.handleSystemBack()
                    } else {
                        Toast.makeText(requireContext(), "일부 업로드 실패", Toast.LENGTH_SHORT).show()
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

    private fun openGalleryBottomSheet() {
        (parentFragmentManager.findFragmentByTag("gallery") as? GalleryBottomSheet)?.let {
            if (it.dialog?.isShowing == true) return
            it.dismissAllowingStateLoss()
        }
        GalleryBottomSheet.newInstance(ArrayList(selectedUris))
            .show(parentFragmentManager, "gallery")
    }

    // 칩 렌더링
    private fun renderPreviewChips(root: View, payload: List<String>) {
        val cg = root.findViewById<ChipGroup>(R.id.chipGroupCategoryPreview)
        cg.removeAllViews()

        if (payload.isEmpty()) {
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

        payload.forEach { raw ->
            val label = when {
                raw.startsWith("regions:") -> {
                    val code = raw.removePrefix("regions:")
                    if (code.contains(":")) {
                        val (sido, sigungu) = code.split(":", limit = 2)
                        "$sido $sigungu"
                    } else code
                }
                raw.startsWith("moods:") -> raw.removePrefix("moods:")
                raw.startsWith("foods:") -> raw.removePrefix("foods:")
                raw.startsWith("companions:") -> raw.removePrefix("companions:")
                else -> raw
            }
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                includeFontPadding = false
                setEnsureMinTouchTargetSize(false)
                chipMinHeight = root.dp(28f)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFE7E7"))
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#C64132"))
                chipStrokeWidth = root.dp(1f)
                setTextColor(Color.parseColor("#C64132"))
                textSize = 12f
            }
            cg.addView(chip)
        }
    }


    private fun ensureMediaPerms() {
        val perms = when {
            Build.VERSION.SDK_INT >= 33 ->
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            Build.VERSION.SDK_INT >= 29 ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_MEDIA_LOCATION)
            else ->
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { } .launch(perms)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CONTENT, getFeedContent())
    }

    private fun getFeedContent(): String =
        binding.etContent.text?.toString()?.trim().orEmpty()

    private fun highlightHashtags(editable: Editable) {
        val old = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
        for (span in old) {
            if (span.foregroundColor == HASHTAG_COLOR) editable.removeSpan(span)
        }
        val regex = Regex("""#([^\s#]+)""")
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

    private fun extractHashtags(text: String): List<String> =
        Regex("""#([^\s#]+)""")
            .findAll(text)
            .map { it.groupValues[1] }
            .toList()

    data class SelectedCategories(
        val moods: List<String>,
        val foods: List<String>,
        val companions: List<String>
    )
}
