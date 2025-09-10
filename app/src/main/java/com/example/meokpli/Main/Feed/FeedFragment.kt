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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.*
import com.example.meokpli.R
import com.example.meokpli.databinding.FragmentFeedBinding
import com.example.meokpli.gallery.GalleryBottomSheet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Collections

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val TAG = "FeedFragment"

    // --- binding ---
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    // --- UI refs ---
    private lateinit var cameraBtn: View
    private lateinit var backBtn: View
    private lateinit var uploadBtn: View
    private lateinit var rvPhotos: RecyclerView
    private lateinit var photosAdapter: SelectedPhotosAdapter
    private lateinit var feedApi: MainApi

    // --- state ---
    private val selectedUris = mutableListOf<Uri>() // 사진 선택 목록
    private var sel = SelectedCategories(emptyList(), emptyList(), emptyList())
    private var selectedPayload: ArrayList<String> = arrayListOf() // 서버 전송용
    private var hashtagWatcher: TextWatcher? = null
    private var latestContent: String = "" // <- EditText 추적용

    private val HASHTAG_COLOR = Color.parseColor("#FF0000")
    private val STATE_CONTENT = "state_feed_content"

    // dp helper
    private fun View.dp(v: Float) = v * resources.displayMetrics.density

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

        // 버튼/리사이클러뷰 바인딩
        backBtn = view.findViewById(R.id.btnBack)
        cameraBtn = view.findViewById(R.id.btnCamera)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        uploadBtn = view.findViewById(R.id.btnUpload)

        // 내용 복원
        savedInstanceState?.getString(STATE_CONTENT)?.let { restored ->
            latestContent = restored
            binding.etContent.setText(restored)
            binding.etContent.setSelection(restored.length)
        }

        // 리사이클러뷰
        rvPhotos.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvPhotos.setHasFixedSize(true)

        // 실시간 해시태그 색칠
        hashtagWatcher = object : TextWatcher {
            private var running = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (running || s == null) return
                running = true
                try {
                    highlightHashtags(s)
                    latestContent = s.toString() // <- 매번 업데이트
                } finally {
                    running = false
                }
            }
        }
        binding.etContent.addTextChangedListener(hashtagWatcher)

        // 사진 어댑터/터치헬퍼
        setupRecycler(view)

        // 갤러리 선택 리스너
        setupGalleryListener(view)

        // 카테고리 다이얼로그 결과
        setupCategoryListener(view)

        // 카테고리 다이얼로그 열기 버튼
        view.findViewById<TextView>(R.id.btnCategoryAdd)?.setOnClickListener {
            CategorySelectDialog.newInstance(
                ArrayList(sel.moods), ArrayList(sel.foods), ArrayList(sel.companions)
            ).show(parentFragmentManager, "CategorySelectDialog")
        }

        // 업로드 버튼
        setupUpload(view)

        // 뒤로/카메라 버튼
        cameraBtn.setOnClickListener { openGalleryBottomSheet() }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }
    }

    // ---------------- util ----------------

    private fun setupRecycler(view: View) {
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
    }

    private fun setupGalleryListener(view: View) {
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
            Toast.makeText(requireContext(), "선택: ${uris.size}장", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoryListener(view: View) {
        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            selectedPayload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD) ?: arrayListOf()
            sel = SelectedCategories(
                b.getStringArrayList(CategorySelectDialog.KEY_MOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.KEY_FOODS) ?: arrayListOf(),
                b.getStringArrayList(CategorySelectDialog.KEY_COMPANIONS) ?: arrayListOf()
            )
            renderPreviewChips(view, sel)
        }
    }

    private fun setupUpload(view: View) {
        uploadBtn.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(requireContext(), "사진을 최소 1장 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val contentText = latestContent.trim().ifEmpty { null }
            // TODO: feedApi 업로드 로직 (생략)
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

    private fun highlightHashtags(editable: Editable) {
        val old = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
        for (span in old) {
            if (span.foregroundColor == HASHTAG_COLOR) {
                editable.removeSpan(span)
            }
        }
        val regex = Regex("""#([^\s#]+)""")
        regex.findAll(editable.toString()).forEach { m ->
            editable.setSpan(
                ForegroundColorSpan(HASHTAG_COLOR),
                m.range.first, m.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // 미리보기 칩 생성
    private fun renderPreviewChips(root: View, s: SelectedCategories) {
        val cg = root.findViewById<ChipGroup>(R.id.chipGroupCategoryPreview)
        cg.removeAllViews()
        val labels = s.moods + s.foods + s.companions
        if (labels.isEmpty()) {
            val empty = Chip(requireContext()).apply {
                text = "선택 없음"
                isCheckable = false
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#EEEEEE"))
                setTextColor(Color.parseColor("#888888"))
            }
            cg.addView(empty)
            return
        }
        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                setTextColor(Color.parseColor("#C64132"))
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFE7E7"))
            }
            cg.addView(chip)
        }
    }

    // 권한
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
        ) { }.launch(perms)
    }

    // 상태 저장
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CONTENT, latestContent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hashtagWatcher = null
    }

    data class SelectedCategories(
        val moods: List<String>,
        val foods: List<String>,
        val companions: List<String>
    )
}
