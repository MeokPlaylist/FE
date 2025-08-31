package com.example.meokpli.Main.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.R

class PhotoAdapter(
    private val photos: List<String>,
    private val isRegionMode: Boolean = false
) : RecyclerView.Adapter<PhotoAdapter.PhotoVH>() {

    inner class PhotoVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView? = v.findViewById(R.id.ivThumbnail)
        val ivRegionThumb: ImageView? = v.findViewById(R.id.ivRegionThumbnail)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
        val layoutId = if (isRegionMode) {
            R.layout.item_feed_region_thumbnail
        } else {
            R.layout.item_my_feed_thumbnail
        }
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return PhotoVH(v)
    }

    override fun onBindViewHolder(holder: PhotoVH, position: Int) {
        if (isRegionMode) {
            // 지역별 모드
            holder.ivRegionThumb?.let { iv ->
                val params = iv.layoutParams
                params.width = 480   // 원하는 dp 크기(px 변환해서 넣는 게 안전)
                params.height = 480
                iv.layoutParams = params

                iv.load(photos[position]) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                }
            }
        } else {
            // ✅ 기간별 모드
            val screenWidth = holder.itemView.resources.displayMetrics.widthPixels
            val params = holder.ivThumb?.layoutParams
            params?.width = screenWidth / 2   // 2분할
            params?.height = params?.width!!     // 정사각형
            holder.ivThumb?.layoutParams = params

            holder.ivThumb?.load(photos[position]) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
            }
        }
    }
    override fun getItemCount() = photos.size
}