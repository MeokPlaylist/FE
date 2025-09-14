package com.meokpli.app.Main.Home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.R

class CoverPickAdapter(
    private val images: List<String>,
    initiallySelected: Int = 0
) : RecyclerView.Adapter<CoverPickAdapter.VH>() {

    private var selectedIndex = initiallySelected.coerceIn(0, (images.size - 1).coerceAtLeast(0))

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.img)
        val ivCheck: ImageView = v.findViewById(R.id.ivCheck)
        val ratioBox: FrameLayout = v.findViewById(R.id.ratioBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cover_pick, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, @SuppressLint("RecyclerView") pos: Int) {
        h.img.load(images[pos])

        // 정사각형 유지 (그리드 셀의 폭에 맞춰 높이 = 폭)
        h.itemView.post {
            val w = h.itemView.width
            val params = h.ratioBox.layoutParams
            params.height = w
            h.ratioBox.layoutParams = params
        }

        val checked = (pos == selectedIndex)
        h.ivCheck.setImageResource(
            if (checked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
        )

        h.itemView.setOnClickListener {
            if (selectedIndex == pos) return@setOnClickListener
            val prev = selectedIndex
            selectedIndex = pos
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
        }
    }

    override fun getItemCount(): Int = images.size
    fun getSelectedIndex(): Int = selectedIndex
}
