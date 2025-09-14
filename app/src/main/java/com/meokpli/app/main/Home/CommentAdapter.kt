package com.meokpli.app.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.main.Home.UiComment
import com.meokpli.app.R

class CommentAdapter(
    private val onReplyClick: (UiComment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    // DiffUtil 변경: UiComment 전용
    private val diff = object : DiffUtil.ItemCallback<UiComment>() {
        override fun areItemsTheSame(old: UiComment, new: UiComment): Boolean {
            return (old.author + old.content + old.createdAt) ==
                    (new.author + new.content + new.createdAt)
        }
        override fun areContentsTheSame(old: UiComment, new: UiComment) = old == new
    }

    private val differ = AsyncListDiffer(this, diff)
    val currentList get() = differ.currentList
    fun submitList(list: List<UiComment>) = differ.submitList(list)

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.ivAvatar)
        val name: TextView = v.findViewById(R.id.tvName)
        val date: TextView = v.findViewById(R.id.tvDate)
        val body: TextView = v.findViewById(R.id.tvBody)
        val tvReplyHint: TextView = v.findViewById(R.id.tvReplyHint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val comment = currentList[position]
        holder.name.text = comment.author
        holder.date.text = comment.createdAt
        holder.body.text = comment.content

        // 프로필 이미지 로드
        if (comment.avatarUrl.isNullOrBlank()) {
            holder.avatar.setImageResource(R.drawable.ic_profile_red)
        } else {
            holder.avatar.load(comment.avatarUrl)
        }

        // 답글 클릭 이벤트
        holder.tvReplyHint.setOnClickListener { onReplyClick(comment) }
    }

    override fun getItemCount() = currentList.size
}
