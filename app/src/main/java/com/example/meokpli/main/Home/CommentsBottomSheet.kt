package com.example.meokpli.comments

import android.app.AlertDialog
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
import com.example.meokpli.auth.Network
import com.example.meokpli.main.Home.Comment
import com.example.meokpli.main.Home.CommentPostRequest
import com.example.meokpli.main.Home.CommentUpdateRequest
import com.example.meokpli.main.Home.CommentApi
import com.example.meokpli.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // ✅ 분리된 CommentApi 사용
    private val api: CommentApi by lazy { Network.commentApi(requireContext()) }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return i.inflate(R.layout.bottom_sheet_comments, c, false)
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        et = v.findViewById(R.id.etComment)
        recycler = v.findViewById(R.id.recyclerComments)

        adapter = CommentAdapter(
            onReplyClick = { c ->
                // 멘션 UX + 키보드 포커스
                et.setText("@${c.author} ")
                et.setSelection(et.text.length)
                et.requestFocus()
                showKeyboard()
            },
            onEditClick = { c -> showEditDialog(c) },
            onDeleteClick = { c -> confirmDelete(c) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // IME "보내기"로 전송
        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = et.text.toString().trim()
                if (text.isNotEmpty()) postComment(text)
                true
            } else false
        }

        // 자동 포커스를 원하면 주석 해제
        // et.requestFocus(); showKeyboard()

        loadComments()
    }

    private fun loadComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val page = api.getComments(feedId)
                adapter.submitList(page.content)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "댓글 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postComment(text: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val created = api.postComment(feedId, CommentPostRequest(text))
                val newList = adapter.currentList.toMutableList().apply { add(created) }
                adapter.submitList(newList)
                recycler.scrollToPosition(newList.lastIndex)
                et.setText("")
                hideKeyboard()

                // 부모 프래그먼트에 개수 업데이트 알림
                setFragmentResult("comment_result", bundleOf("feedId" to feedId, "count" to newList.size))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "등록 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(comment: Comment) {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            setText(comment.content)
            setSelection(text.length)
            maxLines = 5
        }
        AlertDialog.Builder(ctx)
            .setTitle("댓글 수정")
            .setView(input)
            .setPositiveButton("저장") { d, _ ->
                val newText = input.text.toString().trim()
                if (newText.isEmpty()) {
                    Toast.makeText(ctx, "내용을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        api.updateComment(comment.id, CommentUpdateRequest(newText))
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            val list = adapter.currentList.toMutableList()
                            val idx = list.indexOfFirst { it.id == comment.id }
                            if (idx >= 0) {
                                list[idx] = list[idx].copy(content = newText)
                                adapter.submitList(list)
                            }
                        }
                    }.onFailure { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(ctx, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                d.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmDelete(comment: Comment) {
        val ctx = requireContext()
        android.app.AlertDialog.Builder(ctx)
            .setMessage("이 댓글을 삭제할까요?")
            .setPositiveButton("삭제") { d, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { api.deleteComment(comment.id) }
                        .onSuccess {
                            withContext(Dispatchers.Main) {
                                val list = adapter.currentList.toMutableList()
                                val idx = list.indexOfFirst { it.id == comment.id }
                                if (idx >= 0) {
                                    list.removeAt(idx)
                                    adapter.submitList(list)
                                }
                            }
                        }
                        .onFailure { e ->
                            withContext(Dispatchers.Main) {
                                Toast.makeText(ctx, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                d.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
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
