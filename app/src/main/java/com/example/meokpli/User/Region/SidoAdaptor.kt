package com.example.meokpli.User.Region

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class SidoAdapter(
    private val items: List<String>,
    private var selected: String?,
    private val onSidoSelected: (String) -> Unit
) : RecyclerView.Adapter<SidoAdapter.VH>() {

    fun setSelected(value: String?) {
        selected = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_region, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        holder.txt.text = name

        val isSelected = name == selected
        holder.txt.setBackgroundResource(if (isSelected) R.drawable.bg_selected else R.drawable.bg_unselected)
        holder.txt.setTextColor(holder.txt.resources.getColor(if (isSelected) android.R.color.white else android.R.color.black, null))

        holder.itemView.setOnClickListener {
            if (selected != name) {
                selected = name
                notifyDataSetChanged()
                onSidoSelected(name)
            }
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txt: TextView = v.findViewById(R.id.textRegion)
    }
}
