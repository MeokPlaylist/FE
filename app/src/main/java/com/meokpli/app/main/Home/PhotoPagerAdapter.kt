package com.meokpli.app.main.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.R

class PhotoPagerAdapter(
    val images: List<String>
) : RecyclerView.Adapter<PhotoPagerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_detail_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        h.iv.load(images[pos])
    }

    override fun getItemCount(): Int = images.size
}
