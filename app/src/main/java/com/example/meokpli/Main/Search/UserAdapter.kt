package com.example.meokpli.Main.Search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.example.meokpli.User.UserSearchDto

class UserAdapter(
    private val items: MutableList<UserSearchDto>,
    private val onItemClick: (UserSearchDto) -> Unit // ✅ 클릭 콜백 추가
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<UserSearchDto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class UserViewHolder(
        itemView: View,
        private val onItemClick: (UserSearchDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvNickname: TextView = itemView.findViewById(R.id.tvNickname)
        private val tvIntro: TextView = itemView.findViewById(R.id.tvIntro)
        private val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)

        fun bind(user: UserSearchDto) {
            tvNickname.text = user.nickname
            tvIntro.text = user.introduction ?: ""
            // ivProfile.setImage... (Glide/Coil 같은 걸로 프로필 이미지 로딩)

            itemView.setOnClickListener { onItemClick(user) } // ✅ 클릭 이벤트
        }
    }
}
