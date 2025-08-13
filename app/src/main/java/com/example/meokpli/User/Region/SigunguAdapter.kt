package com.example.meokpli.User.Region

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class SigunguAdapter(
    private var items: List<String>,
    private val selected: MutableSet<String>,
    private val maxCount: Int = 10,
    private val onToggle: (String, Boolean) -> Unit,
    private val onLimitExceeded: () -> Unit
) : RecyclerView.Adapter<SigunguAdapter.VH>() {

    fun submit(newList: List<String>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region_sigungu, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        holder.txt.text = name
        val isSelected = selected.contains(name)

        holder.check.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.txt.setTextColor(
            holder.txt.resources.getColor(
                if (isSelected) R.color.red_primary else android.R.color.black, null
            )
        )

        holder.itemView.setOnClickListener {
            val nowSelected = selected.contains(name)
            if (nowSelected) {
                selected.remove(name)
                notifyItemChanged(position)
                onToggle(name, false)
            } else {
                if (selected.size >= maxCount) {
                    onLimitExceeded()
                } else {
                    selected.add(name)
                    notifyItemChanged(position)
                    onToggle(name, true)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txt: TextView = v.findViewById(R.id.textRegion)
        val check: TextView = v.findViewById(R.id.checkView)
    }
}
