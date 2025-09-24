package com.meokpli.app.main.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.R

class PhotoAdapter(
    private val photos: List<MyPageItem.Photo>,
    private val isRegionMode: Boolean = false,
    private val onPhotoClick: (Long) -> Unit = {}
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
        val photo = photos[position]

        if (isRegionMode) {
            // 지역별 모드
            holder.ivRegionThumb?.let { iv ->
                val params = iv.layoutParams
                params.width = 480   // 원하는 dp 크기(px 변환해서 넣는 게 안전)
                params.height = 480
                iv.layoutParams = params

                iv.load(photo.url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                }
                iv.setOnClickListener {
                    onPhotoClick(photo.feedId)
                }
            }
        } else {
            // 기간별 모드
            val photo = photos[position]  // feedId + url
            val screenWidth = holder.itemView.resources.displayMetrics.widthPixels
            val params = holder.ivThumb?.layoutParams
            params?.width = screenWidth / 2   // 2분할
            params?.height = params?.width!!  // 정사각형
            holder.ivThumb?.layoutParams = params

            holder.ivThumb?.load(photo.url) {   // url 로드
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
            }
            holder.ivThumb?.setOnClickListener {
                onPhotoClick(photo.feedId)      // 클릭 시 feedId 전달
            }
        }
    }
    override fun getItemCount() = photos.size
}