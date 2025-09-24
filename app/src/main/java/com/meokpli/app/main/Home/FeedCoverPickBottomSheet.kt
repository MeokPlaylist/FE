package com.meokpli.app.main.Home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FeedCoverPickBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY = "cover_pick_result"
        const val KEY_FEED_ID = "feedId"
        const val KEY_NEW_INDEX = "newIndex"
        const val KEY_OLD_INDEX = "oldIndex"

        private const val ARG_FEED_ID = "arg_feed_id"
        private const val ARG_IMAGES = "arg_images"
        private const val ARG_CURRENT_INDEX = "arg_current_index"

        fun newInstance(feedId: Long, images: ArrayList<String>, currentMainIndex: Int) =
            FeedCoverPickBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_FEED_ID, feedId)
                    putStringArrayList(ARG_IMAGES, images)
                    putInt(ARG_CURRENT_INDEX, currentMainIndex)
                }
            }
    }

    private var feedId: Long = 0L
    private var images: List<String> = emptyList()
    private var oldIndex: Int = 0

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CoverPickAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_cover_pick, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        feedId = requireArguments().getLong(ARG_FEED_ID)
        images = requireArguments().getStringArrayList(ARG_IMAGES) ?: emptyList()
        oldIndex = requireArguments().getInt(ARG_CURRENT_INDEX, 0)

        recycler = view.findViewById(R.id.recyclerPhotos)
        val tvCancel = view.findViewById<TextView>(R.id.tvCancel)
        val tvDone = view.findViewById<TextView>(R.id.tvDone)

        adapter = CoverPickAdapter(images, initiallySelected = oldIndex)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.addItemDecoration(GridSpacingDecoration(3, 6))
        recycler.adapter = adapter

        tvCancel.setOnClickListener { dismiss() }
        tvDone.setOnClickListener {
            val newIndex = adapter.getSelectedIndex()
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_FEED_ID to feedId,
                    KEY_NEW_INDEX to newIndex,
                    KEY_OLD_INDEX to oldIndex
                )
            )
            dismiss()
        }

        // 바텀시트 기본 높이 조금 키우고 둥근 모서리 유지
        dialog?.setOnShowListener {
            val bs = (it as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            BottomSheetBehavior.from(bs).apply {
                isFitToContents = true
                state = BottomSheetBehavior.STATE_EXPANDED
                peekHeight = (resources.displayMetrics.heightPixels * 0.70f).toInt()
            }
        }
    }

    /** 3열 그리드 여백 데코레이션 */
    class GridSpacingDecoration(private val spanCount: Int, private val spacingDp: Int) : RecyclerView.ItemDecoration() {
        private fun Int.dp(v: View) = (this * v.resources.displayMetrics.density).toInt()
        override fun getItemOffsets(outRect: android.graphics.Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
            val spacing = spacingDp.dp(v)
            val pos = parent.getChildAdapterPosition(v)
            val col = pos % spanCount
            outRect.left = spacing - col * spacing / spanCount
            outRect.right = (col + 1) * spacing / spanCount
            if (pos < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        }
    }
}
