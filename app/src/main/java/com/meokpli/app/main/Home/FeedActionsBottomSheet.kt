package com.meokpli.app.main.Home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import com.meokpli.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FeedActionsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY = "feed_actions_result"
        const val KEY_ACTION = "action"
        const val KEY_FEED_ID = "feedId"

        const val ACTION_EDIT_POST = "edit_post"
        const val ACTION_EDIT_COVER = "edit_cover"
        const val ACTION_EDIT_CATEGORY = "edit_category"
        const val ACTION_DELETE = "delete"

        fun newInstance(feedId: Long) = FeedActionsBottomSheet().apply {
            arguments = Bundle().apply { putLong(KEY_FEED_ID, feedId) }
        }
    }

    private val feedId by lazy { requireArguments().getLong(KEY_FEED_ID) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ★ 여기서 레이아웃만 inflate (BottomSheetDialog 직접 사용 X)
        return inflater.inflate(R.layout.bottom_sheet_feed_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.rowEditPost)?.setOnClickListener {
            setResult(ACTION_EDIT_POST)
        }
        view.findViewById<LinearLayout>(R.id.rowEditCover)?.setOnClickListener {
            setResult(ACTION_EDIT_COVER)
        }
        view.findViewById<LinearLayout>(R.id.rowEditCategory)?.setOnClickListener {
            setResult(ACTION_EDIT_CATEGORY)
        }
        view.findViewById<LinearLayout>(R.id.rowDelete)?.setOnClickListener {
            setResult(ACTION_DELETE)
        }
    }

    private fun setResult(action: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(KEY_ACTION to action, KEY_FEED_ID to feedId)
        )
        dismiss()
    }
}
