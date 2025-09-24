package com.meokpli.app.main.Home

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
            }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

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

        loadComments()
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

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, 0)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et.windowToken, 0)
    }
}
