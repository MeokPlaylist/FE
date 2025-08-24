package com.example.meokpli.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class FeedAdapter(private val items: List<Feed>) : RecyclerView.Adapter<FeedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvUser: TextView = v.findViewById(R.id.tvUserName)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvCaption: TextView = v.findViewById(R.id.tvCaption)
        val imgPhoto: ImageView = v.findViewById(R.id.imgPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.tvUser.text = item.userName
        h.tvDate.text = item.date
        h.tvCaption.text = item.caption
        h.imgPhoto.setImageResource(item.imageRes)
    }

    override fun getItemCount() = items.size
}
data class Feed(
    val userName: String,
    val date: String,
    val caption: String,
    val imageRes: Int
)