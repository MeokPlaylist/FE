package com.meokpli.app.main.Home

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch
class CommentsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "CommentsBottomSheet"
        private const val ARG_FEED_ID = "arg_feed_id"
        fun newInstance(feedId: Long) = CommentsBottomSheet().apply {
            arguments = Bundle().apply { putLong(ARG_FEED_ID, feedId) }
        }
    }

    private val feedId by lazy { requireArguments().getLong(ARG_FEED_ID) }

    private lateinit var et: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CommentAdapter
    private lateinit var btnSend: ImageButton

    private val api: CommentApi by lazy { Network.commentApi(requireContext()) }
    private var isSending = false

    private var myNickname: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView feedId=$feedId")
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        et = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSend)
        recycler = view.findViewById(R.id.recyclerComments)

        adapter = CommentAdapter(
            onReplyClick = { c ->
                Log.d(TAG, "onReplyClick author=${c.author}")
                et.setText("@${c.author} ")
                et.setSelection(et.text.length)
                et.requestFocus()
                showKeyboard()
            },
            onMoreClick = { anchor, c ->
                val isMine = !myNickname.isNullOrBlank() && myNickname == c.author
                if (isMine) showCommentDeleteConfirm(c) else showCommentReportMenu(anchor, c)
            },
            myNickname = myNickname
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { Network.userApi(requireContext()).getMyNickName().nickname }
                .onSuccess { nick ->
                    myNickname = nick
                    adapter.updateMyNickname(nick)
                }.onFailure {
                    myNickname = null
                }
        }

        // 텍스트 변경에 따라 전송 버튼 활성화
        et.doOnTextChanged { text, _, _, _ ->
            val hasText = !text.isNullOrBlank()
            btnSend.isEnabled = hasText && !isSending
            btnSend.alpha = if (btnSend.isEnabled) 1f else 0.4f
        }

        // 전송 버튼 클릭
        btnSend.setOnClickListener {
            if (isSending) return@setOnClickListener
            val text = et.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            postComment(text)
        }

        recycler.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
                et.clearFocus()
            }
            false
        }

        loadComments()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            val shape = com.google.android.material.shape.MaterialShapeDrawable(
                com.google.android.material.shape.ShapeAppearanceModel()
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 32f)
                    .setTopRightCorner(CornerFamily.ROUNDED, 32f)
                    .build()
            )
            shape.fillColor = ColorStateList.valueOf(Color.WHITE)
            sheet.background = shape
        }
        return dialog
    }


    /** 댓글 목록 로드 */
    private fun loadComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            val t0 = SystemClock.elapsedRealtime()
            Log.d(TAG, "loadComments start feedId=$feedId")
            try {
                val page = api.getComments(feedId)
                val uiList = page.content.map { it.toUi() }
                Log.i(TAG, "loadComments ok items=${uiList.size} in ${SystemClock.elapsedRealtime() - t0}ms")
                adapter.submitList(uiList)

                // 스크롤 맨 아래로
                if (uiList.isNotEmpty()) {
                    recycler.post { recycler.scrollToPosition(uiList.size - 1) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadComments error", e)
            }
        }
    }

    /** 댓글 작성 */
    private fun postComment(text: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val t0 = SystemClock.elapsedRealtime()
            isSending = true
            btnSend.isEnabled = false
            btnSend.alpha = 0.4f
            Log.d(TAG, "postComment start len=${text.length}")

            try {
                // 서버에서 내 닉네임 조회(필요 시)
                val userApi = Network.userApi(requireContext())
                val myNickname = runCatching { userApi.getMyPage().userNickname ?: "익명" }
                    .getOrElse { "익명" }
                Log.d("myNickname",myNickname)
                api.writeComment(feedId, myNickname, text)
                Log.i(TAG, "postComment ok in ${SystemClock.elapsedRealtime() - t0}ms")

                et.setText("")
                hideKeyboard()

                // 작성 후 목록 새로고침 + 맨 아래로
                loadComments()

                // 상위에 결과 브로드캐스트(옵션)
                parentFragmentManager.setFragmentResult(
                    "comment_result",
                    bundleOf("feedId" to feedId, "added" to true)
                )
            } catch (e: Exception) {
                Log.e(TAG, "postComment error", e)
                Toast.makeText(requireContext(), "댓글 등록 실패", Toast.LENGTH_SHORT).show()
            } finally {
                isSending = false
                // 텍스트 상태에 따라 버튼 상태 재조정
                val hasText = et.text?.isNotBlank() == true
                btnSend.isEnabled = hasText
                btnSend.alpha = if (btnSend.isEnabled) 1f else 0.4f
            }
        }
    }

    private fun showCommentReportMenu(anchor: View, c: UiComment) {
        val v = layoutInflater.inflate(R.layout.popup_feed_report, null, false)
        val popup = PopupWindow(
            v,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        v.findViewById<TextView>(R.id.tvReport).setOnClickListener {
            popup.dismiss()
            showCommentReportConfirm(c)
        }

        showPopupBelowRight(popup, anchor, v)
    }

    private fun showCommentDeleteConfirm(c: UiComment) {
        val v = layoutInflater.inflate(R.layout.dialog_delete_confirm, null, false)
        val btnCancel = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnDelete = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(v).setCancelable(true).create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnDelete.setOnClickListener {
            // ✅ 서버 호출 없음: 로컬 목록만 갱신
            val list = adapter.currentList.toMutableList()

            // CommentAdapter의 areItemsTheSame 기준과 동일한 키로 찾기
            val idx = list.indexOfFirst {
                it.author == c.author && it.content == c.content && it.createdAt == c.createdAt
            }

            if (idx >= 0) {
                list.removeAt(idx)
                // AsyncListDiffer는 "새 리스트 인스턴스"여야 갱신됨 → OK (mutable copy 사용)
                adapter.submitList(list)
                Toast.makeText(requireContext(), "삭제되었습니다. (로컬 반영)", Toast.LENGTH_SHORT).show()
            } else {
                // 못 찾으면 그냥 아무 것도 안 함 (서버 없으므로 재로딩도 생략)
                Toast.makeText(requireContext(), "삭제할 항목을 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    /** 커스텀 확인 다이얼로그 (dialog_report_confirm.xml 재사용) */
    private fun showCommentReportConfirm(c: UiComment) {
        val ctx = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_report_confirm, null, false)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnReport = view.findViewById<MaterialButton>(R.id.btnUnfollow).apply {
            text = "신고"
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnReport.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {


                    Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "신고 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        dialog.show()
        // 둥근 모서리 보이게 기본 배경 제거
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // 필요시: dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showPopupBelowRight(popup: PopupWindow, anchor: View, contentView: View) {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = contentView.measuredWidth
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }


    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, 0)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et.windowToken, 0)
    }
}
