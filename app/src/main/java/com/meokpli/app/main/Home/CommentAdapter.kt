package com.meokpli.app.comments

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.R
import com.meokpli.app.main.Home.UiComment

class CommentAdapter(
    private val onReplyClick: (UiComment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    companion object { private const val TAG = "CommentAdapter" }

    private val diff = object : DiffUtil.ItemCallback<UiComment>() {
        override fun areItemsTheSame(old: UiComment, new: UiComment) =
            (old.author + old.content + old.createdAt) == (new.author + new.content + new.createdAt)
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
        val c = currentList[position]
        Log.v(TAG, "bind pos=$position author='${c.author}' len=${c.content.length}")

        holder.name.text = c.author
        holder.date.text = c.createdAt
        holder.body.text = c.content

        if (c.avatarUrl.isNullOrBlank()) {
            Log.d(TAG, "avatar none for '${c.author}'")
            holder.avatar.setImageResource(R.drawable.ic_profile_red)
        } else {
            Log.d(TAG, "avatar load url=${c.avatarUrl}")
            holder.avatar.load(c.avatarUrl)
        }

        holder.tvReplyHint.setOnClickListener { onReplyClick(c) }
    }

    override fun getItemCount() = currentList.size
}
