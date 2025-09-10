package com.example.meokpli.user.Region

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
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region_sido, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        holder.txt.text = name

        val isSelected = name == selected
        holder.leftIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.txt.setTextColor(
            holder.txt.resources.getColor(
                if (isSelected) R.color.red_primary else android.R.color.black, null
            )
        )

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
        val leftIndicator: View = v.findViewById(R.id.leftIndicator)
    }
}
