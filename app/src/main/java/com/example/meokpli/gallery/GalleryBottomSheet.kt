    package com.example.meokpli.gallery

    import android.Manifest
    import android.content.ContentUris
    import android.net.Uri
    import android.os.Build
    import android.os.Bundle
    import android.provider.MediaStore
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.core.os.bundleOf
    import androidx.recyclerview.widget.GridLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.meokpli.R
    import com.google.android.material.appbar.MaterialToolbar
    import com.google.android.material.bottomsheet.BottomSheetBehavior
    import com.google.android.material.bottomsheet.BottomSheetDialog
    import com.google.android.material.bottomsheet.BottomSheetDialogFragment

    class GalleryBottomSheet : BottomSheetDialogFragment() {

        companion object {
            const val RESULT_KEY = "gallery_result"
            const val RESULT_URIS = "uris"
            const val MAX_SELECT = 10

            private const val ARG_PRESELECTED = "arg_preselected"
            private const val STATE_SELECTED = "state_selected"

            fun newInstance(preselected: ArrayList<Uri> = arrayListOf()): GalleryBottomSheet {
                return GalleryBottomSheet().apply {
                    arguments = bundleOf(ARG_PRESELECTED to preselected)
                }
            }
        }

        private lateinit var adapter: GalleryAdapter
        private val allUris = mutableListOf<Uri>()

        // 순서 보존 + 중복 제거
        private val selected = LinkedHashSet<Uri>()

        private val requestPerms = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val ok = granted.values.all { it }
            if (ok) loadImages() else {
                Toast.makeText(requireContext(), "사진 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?) =
            (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
                setOnShowListener {
                    val sheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    val behavior = BottomSheetBehavior.from(sheet!!)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = false
                    behavior.isHideable = true
                }
            }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
            return inflater.inflate(R.layout.activity_gallery, container, false)
        }

        override fun onViewCreated(view: View, s: Bundle?) {
            val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
            val tvCancel = view.findViewById<TextView>(R.id.tv_cancel)
            val tvDone = view.findViewById<TextView>(R.id.tv_done)
            val rv = view.findViewById<RecyclerView>(R.id.rv)


            // 초기 선택 복원: savedInstanceState > arguments 순서
            val restored = s?.getParcelableArrayList<Uri>(STATE_SELECTED)
                ?: arguments?.getParcelableArrayList(ARG_PRESELECTED)
                ?: arrayListOf()
            selected.clear()
            selected.addAll(restored)

            toolbar.setNavigationOnClickListener { dismiss() }
            tvCancel.setOnClickListener { dismiss() }
            tvDone.alpha = if (selected.isNotEmpty()) 1f else 0.6f
            tvDone.setOnClickListener {
                val result = Bundle().apply {
                    // LinkedHashSet → ArrayList (선택 순서 유지)
                    putParcelableArrayList(RESULT_URIS, ArrayList(selected))
                }
                parentFragmentManager.setFragmentResult(RESULT_KEY, result)
                dismiss()
            }

            rv.layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = GalleryAdapter(
                items = allUris,
                isSelected = { uri -> selected.contains(uri) },
                orderOf = { uri -> selected.indexOf(uri).takeIf { it >= 0 }?.plus(1) },
                onToggle = { uri ->
                    if (selected.contains(uri)) {
                        // 해제
                        selected.remove(uri)
                    } else {
                        // 선택 (최대 개수 제한)
                        if (selected.size >= MAX_SELECT) {
                            Toast.makeText(requireContext(), "최대 ${MAX_SELECT}장까지 선택할 수 있어요", Toast.LENGTH_SHORT).show()
                            return@GalleryAdapter
                        }
                        selected.add(uri) // 맨 뒤에 추가 → 선택 순서 유지
                    }
                    adapter.notifyDataSetChanged()
                    tvDone.alpha = if (selected.isNotEmpty()) 1f else 0.6f
                }
            )
            rv.adapter = adapter
            ensurePermissionThenLoad()
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            // 회전 등 구성 변경 대비: 현재 선택 저장
            outState.putParcelableArrayList(STATE_SELECTED, ArrayList(selected))
        }

        private fun ensurePermissionThenLoad() {
            val perms = if (Build.VERSION.SDK_INT >= 33)
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

            requestPerms.launch(perms)
        }

        private fun loadImages() {
            allUris.clear()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { c ->
                val idxId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext()) {
                    val id = c.getLong(idxId)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    allUris.add(uri)
                }
            }
            adapter.notifyDataSetChanged()
        }

        // 선택 목록에서의 인덱스 계산 (LinkedHashSet용 간단 헬퍼)
        private fun LinkedHashSet<Uri>.indexOf(target: Uri): Int {
            var i = 0
            for (u in this) {
                if (u == target) return i
                i++
            }
            return -1
        }
    }
