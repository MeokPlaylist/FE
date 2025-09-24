package com.meokpli.app.main.Interaction

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.meokpli.app.R
import com.google.android.material.button.MaterialButton

private const val TAG_FUA = "FollowUserAdapter"

data class FollowUserUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val avatarUrl: String?,
    var isFollowing: Boolean = false,   // 나 → 그
    var followsMe: Boolean = false      // 그 → 나
)

class FollowUserAdapter(
    private val onItemClick: (FollowUserUi) -> Unit,
    private val onActionClick: (FollowUserUi) -> Unit
) : androidx.recyclerview.widget.ListAdapter<FollowUserUi, FollowUserAdapter.ItemVH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FollowUserUi>() {
            override fun areItemsTheSame(old: FollowUserUi, new: FollowUserUi) = old.id == new.id
            override fun areContentsTheSame(old: FollowUserUi, new: FollowUserUi) = old == new
        }
    }

    // 내 닉네임 (내 항목이면 버튼 숨김)
    private var myNickname: String? = null
    fun setMyNickname(nick: String?) {
        myNickname = nick
        notifyDataSetChanged()
        Log.d(TAG_FUA, "setMyNickname=$myNickname")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_follow, parent, false)
        return ItemVH(v)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) = holder.bind(getItem(position))

    inner class ItemVH(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val btn: MaterialButton = itemView.findViewById(R.id.btnAction)
        private val btnUnfollow: ImageView = itemView.findViewById(R.id.btnUnfollow)

        fun bind(u: FollowUserUi) {
            val ctx = itemView.context

            // 아바타
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

            val isSelf = !myNickname.isNullOrBlank() && u.name == myNickname

            if (isSelf) {
                btn.visibility = View.GONE
                btnUnfollow.visibility = View.GONE
            } else {
                when {
                    // 내가 이미 팔로우(맞팔 포함) → X 아이콘
                    u.isFollowing -> {
                        btn.visibility = View.GONE
                        btnUnfollow.visibility = View.VISIBLE
                        btnUnfollow.setImageResource(R.drawable.ic_close)
                        btnUnfollow.contentDescription = "언팔로우"
                        btnUnfollow.setOnClickListener { onActionClick(u) } // 언팔 외부 위임
                    }
                    // 나를 팔로우(나는 아직 아님) → 맞팔로우
                    u.followsMe -> {
                        btn.visibility = View.VISIBLE
                        btnUnfollow.visibility = View.GONE
                        btn.text = "맞팔로우"
                        btn.setBackgroundResource(R.drawable.btn_mutual_follow)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                        btn.setOnClickListener { onActionClick(u) } // 팔로우 외부 위임
                    }
                    // 서로 팔로우 아님 → 팔로우
                    else -> {
                        btn.visibility = View.VISIBLE
                        btnUnfollow.visibility = View.GONE
                        btn.text = "팔로우"
                        btn.setBackgroundResource(R.drawable.btn_basic)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                        btn.setOnClickListener { onActionClick(u) } // 팔로우 외부 위임
                    }
                }
            }

            itemView.setOnClickListener {
                Log.d(TAG_FUA, "row click user=${u.name}")
                onItemClick(u)
            }
            btn.setOnClickListener {
                Log.d(TAG_FUA, "action click user=${u.name} isFollowing=${u.isFollowing}")
                onActionClick(u) // 실제 follow/unFollow API는 Fragment에서 처리
            }
        }
    }
}
