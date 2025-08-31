package com.example.meokpli.Main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
// Coil을 쓰려면 주석 해제
// import coil.load

/** UI에서 쓰는 팔로우 사용자 모델 */
data class FollowUserUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val avatarUrl: String?,
    var isFollowing: Boolean = false, // 나 → 그
    var followsMe: Boolean = false    // 그 → 나
)
class FollowUserAdapter(
    private val onItemClick: (FollowUserUi) -> Unit,
    private val onActionClick: (FollowUserUi) -> Unit
) : ListAdapter<FollowUserUi, FollowUserAdapter.ItemVH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FollowUserUi>() {
            override fun areItemsTheSame(old: FollowUserUi, new: FollowUserUi) = old.id == new.id
            override fun areContentsTheSame(old: FollowUserUi, new: FollowUserUi) = old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_follow, parent, false)
        return ItemVH(v)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val btn: MaterialButton = itemView.findViewById(R.id.btnAction)

        fun bind(u: FollowUserUi) {
            val ctx = itemView.context

            if (!u.avatarUrl.isNullOrBlank()) {
                avatar.load(u.avatarUrl) {
                    placeholder(R.drawable.ic_profile_red)
                    error(R.drawable.ic_profile_red)
                    crossfade(true)
                }
            } else {
                avatar.setImageResource(R.drawable.ic_profile_red)
            }

            name.text = u.name
            subtitle.text = u.subtitle

            // ★ 상태별 텍스트 + 배경 drawable + 텍스트 색
            when {
                u.isFollowing && u.followsMe -> {
                    btn.text = "맞팔로잉"
                    btn.setBackgroundResource(R.drawable.btn_basic)
                    btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                }
                u.isFollowing -> {
                    btn.text = "팔로잉"
                    btn.setBackgroundResource(R.drawable.btn_basic)
                    btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                }
                u.followsMe -> {
                    btn.text = "맞팔로우"
                    btn.setBackgroundResource(R.drawable.btn_mutual_follow)
                    btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                }
                else -> {
                    btn.text = "팔로우"
                    btn.setBackgroundResource(R.drawable.btn_basic)
                    btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                }
            }

            itemView.setOnClickListener { onItemClick(u) }
            btn.setOnClickListener { onActionClick(u) }
        }
    }
}