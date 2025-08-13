package com.example.meokpli.gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.R

class GalleryAdapter(
    private val items: List<Uri>,
    private val isSelected: (Uri) -> Boolean,
    private val orderOf: (Uri) -> Int?,
    private val onToggle: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.iv)
        val vBorder: View = v.findViewById(R.id.v_border)
        val tvBadge: TextView = v.findViewById(R.id.tv_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val uri = items[position]
        h.iv.load(uri) { crossfade(true) }

        val sel = isSelected(uri)
        h.vBorder.visibility = if (sel) View.VISIBLE else View.GONE

        val order = orderOf(uri)
        if (order != null) {
            h.tvBadge.visibility = View.VISIBLE
            h.tvBadge.text = order.toString()
        } else {
            h.tvBadge.visibility = View.GONE
        }

        h.itemView.setOnClickListener { onToggle(uri) }
    }

    override fun getItemCount() = items.size
}
