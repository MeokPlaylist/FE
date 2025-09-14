package com.meokpli.app.comments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.auth.Network
import com.meokpli.app.main.Home.CommentApi
import com.meokpli.app.main.Home.toUi
import com.meokpli.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CommentsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_FEED_ID = "arg_feed_id"
        fun newInstance(feedId: Long) = CommentsBottomSheet().apply {
            arguments = Bundle().apply { putLong(ARG_FEED_ID, feedId) }
        }
    }

    private val feedId by lazy { requireArguments().getLong(ARG_FEED_ID) }
    private lateinit var et: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CommentAdapter

    private val api: CommentApi by lazy { Network.commentApi(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        et = view.findViewById(R.id.etComment)
        recycler = view.findViewById(R.id.recyclerComments)

        adapter = CommentAdapter(
            onReplyClick = { comment ->
                et.setText("@${comment.author} ")
                et.setSelection(et.text.length)
                et.requestFocus()
                showKeyboard()
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // IME "보내기" 전송 처리
        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = et.text.toString().trim()
                if (text.isNotEmpty()) {
                    postComment(text)
                }
                true
            } else false
        }

        loadComments()
    }

    /** 댓글 목록 로드 */
    private fun loadComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val page = api.getComments(feedId)
                val uiList = page.content.map { it.toUi() }
                adapter.submitList(uiList)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "댓글 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 댓글 작성 */
    private fun postComment(text: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ✅ 서버에서 내 닉네임 가져오기
                val userApi = Network.userApi(requireContext())
                val myNickname = userApi.getPersonalInfo().name ?: "익명"

                api.writeComment(feedId, myNickname, text)

                et.setText("")
                hideKeyboard()
                loadComments() // 작성 후 새로고침

                setFragmentResult("comment_result", bundleOf("feedId" to feedId, "count" to adapter.currentList.size + 1))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "댓글 등록 실패", Toast.LENGTH_SHORT).show()
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
