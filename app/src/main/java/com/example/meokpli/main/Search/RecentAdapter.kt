package com.example.meokpli.main.Search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class RecentAdapter(
    private val onClick: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent, parent, false)
        return RecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val keyword = items[position]
        holder.bind(keyword, onClick, onDelete)
    }

    override fun getItemCount() = items.size

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        private val tvKeyword: TextView = itemView.findViewById(R.id.tvKeyword)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(
            keyword: String,
            onClick: (String) -> Unit,
            onDelete: (String) -> Unit
        ) {
            tvKeyword.text = keyword
            ivProfile.setImageResource(R.drawable.ic_loupe_checked)

            // 검색어 클릭 → 검색 실행
            itemView.setOnClickListener { onClick(keyword) }

            // 삭제 버튼 클릭 → 삭제 실행
            btnDelete.setOnClickListener { onDelete(keyword) }
        }
    }
}
