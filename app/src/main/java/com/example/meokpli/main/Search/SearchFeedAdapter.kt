package com.example.meokpli.main.Search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.example.meokpli.R
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable

class SearchFeedAdapter(
    private var items: MutableList<SearchedFeed>,
    private val onFeedClick: (feedId: Long) -> Unit
) : RecyclerView.Adapter<SearchFeedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.ivThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_feed_thumbnail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]

        val shimmer = Shimmer.AlphaHighlightBuilder()
            .setDuration(1000)
            .setBaseAlpha(0.7f)
            .setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        val shimmerDrawable = ShimmerDrawable().apply { setShimmer(shimmer) }

        // ✅ photoUrl만 Coil에 넘겨야 함
        holder.img.load(item.photoUrl) {
            crossfade(true)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            placeholder(shimmerDrawable)
        }
    }


    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SearchedFeed>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
