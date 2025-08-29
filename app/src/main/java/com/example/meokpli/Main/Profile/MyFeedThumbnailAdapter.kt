package com.example.meokpli.Main.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.R

class MyFeedThumbnailAdapter(
    private val items: List<MyPageItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PHOTO = 1
    }

    inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvYear: TextView = v.findViewById(R.id.tvYear)
    }

    inner class PhotoVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivThumbnail)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is MyPageItem.YearHeader -> TYPE_HEADER
            is MyPageItem.Photo -> TYPE_PHOTO
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_year_header, parent, false)
            HeaderVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_my_feed_thumbnail, parent, false)
            PhotoVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MyPageItem.YearHeader -> (holder as HeaderVH).tvYear.text = item.year.toString()
            is MyPageItem.Photo -> {
                val vh = holder as PhotoVH
                vh.ivThumb.load(item.url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                }
            }
        }
    }

    override fun getItemCount() = items.size
}