package com.example.meokpli.Main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
// Coil을 쓰려면 주석 해제
// import coil.load

/** UI에서 쓰는 팔로우 사용자 모델 */
data class FollowUserUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val avatarUrl: String? = null
)

class FollowUserAdapter(
    private val onItemClick: (FollowUserUi) -> Unit,
    private val onActionClick: (FollowUserUi) -> Unit
) : ListAdapter<FollowUserUi, FollowUserAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FollowUserUi>() {
            override fun areItemsTheSame(a: FollowUserUi, b: FollowUserUi) = a.id == b.id
            override fun areContentsTheSame(a: FollowUserUi, b: FollowUserUi) = a == b
        }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.imgAvatar)
        val name: TextView = view.findViewById(R.id.tvName)
        val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btn: ImageButton = view.findViewById(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_follow, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)
        h.name.text = item.name
        h.subtitle.text = item.subtitle

        // 이미지 로더 (선택)
        if (item.avatarUrl.isNullOrBlank()) {
            h.avatar.setImageResource(R.drawable.ic_profile_red)
        } else {
            // Coil 사용 시:
            // h.avatar.load(item.avatarUrl) { placeholder(R.drawable.ic_profile_red) }
            h.avatar.setImageResource(R.drawable.ic_profile_red)
        }

        h.itemView.setOnClickListener { onItemClick(item) }
        h.btn.setOnClickListener { onActionClick(item) }
    }
}

