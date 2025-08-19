package com.example.meokpli.Main

import SelectedPhotosAdapter
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.example.meokpli.gallery.GalleryBottomSheet
import java.util.Collections

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private lateinit var cameraBtn: View
    private lateinit var backBtn: View
    private lateinit var rvPhotos: RecyclerView
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var photosAdapter: SelectedPhotosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backBtn = view.findViewById(R.id.btnBack)
        cameraBtn = view.findViewById(R.id.btnCamera)
        rvPhotos = view.findViewById(R.id.rvPhotos)

        rvPhotos.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvPhotos.setHasFixedSize(true)

        // 1) ItemTouchHelper 콜백: 롱프레스 자동 드래그 비활성화
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false    // ★ 핵심

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
            onItemClick = { _, _ -> openGalleryForRepick() },
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
        // ✅ 결과 리스너: parentFragmentManager로 통일
        parentFragmentManager.setFragmentResultListener(
            GalleryBottomSheet.RESULT_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val uris = bundle.getParcelableArrayList<Uri>(GalleryBottomSheet.RESULT_URIS) ?: arrayListOf()
            selectedUris.clear()
            selectedUris.addAll(uris)                           // 바텀시트가 순서 보존해서 돌려줌
            photosAdapter.submitList(selectedUris.toList())

            view.findViewById<View>(R.id.emptyPhotoBox)?.visibility =
                if (selectedUris.isEmpty()) View.VISIBLE else View.GONE

            // 첫 항목으로 스크롤(선택 후 바로 보이게)
            if (selectedUris.isNotEmpty()) rvPhotos.scrollToPosition(0)
        }

        // ▼ 갤러리 바텀시트 열기 (최초/재선택 공통)
        cameraBtn.setOnClickListener { openGalleryForRepick() }

        backBtn.setOnClickListener {
            (requireActivity() as MainActivity).handleSystemBack()
        }
    }

    private fun openGalleryForRepick() {
        // 중복 표시 방지
        (parentFragmentManager.findFragmentByTag("gallery") as? GalleryBottomSheet)?.let {
            if (it.dialog?.isShowing == true) return
            it.dismissAllowingStateLoss()
        }

        // 현재 선택 전달 → 초기 체크 및 순서 유지
        GalleryBottomSheet.newInstance(ArrayList(selectedUris))
            .show(parentFragmentManager, "gallery")
    }
}
