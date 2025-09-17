package com.meokpli.app.main.Roadmap

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import com.meokpli.app.R

import coil.request.ErrorResult
import coil.request.SuccessResult

class RoadmapAdapter(
    private val onPhotoClick: (CallInRoadMapDto) -> Unit
) : RecyclerView.Adapter<RoadmapAdapter.VH>() {

    enum class ItemType { START, MIDDLE, END }

    companion object {
        private const val TAG = "RoadmapAdapter"
    }

    var items: List<CallInRoadMapDto> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_roadmap_timeline, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dto = items[position]
        val type = when {
            itemCount == 1 -> ItemType.END
            position == 0 -> ItemType.START
            position == itemCount - 1 -> ItemType.END
            else -> ItemType.MIDDLE
        }
        Log.d(TAG, "bind pos=$position type=$type name='${dto.name}'")
        holder.bind(dto, type)

        holder.ivPhoto.setOnClickListener {
            onPhotoClick(dto)
        }

        holder.itemView.setOnClickListener {
            Log.i(TAG, "click pos=$position name='${dto.name}' addr='${dto.roadAddressName ?: dto.addressName ?: ""}'")
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lineTop = itemView.findViewById<View>(R.id.lineTop)
        private val lineBottom = itemView.findViewById<View>(R.id.lineBottom)
        private val dot = itemView.findViewById<View>(R.id.dot)
        private val ivArrow = itemView.findViewById<ImageView>(R.id.ivArrow)

        val ivPhoto = itemView.findViewById<ImageView>(R.id.ivPhoto)
        private val tvName = itemView.findViewById<TextView>(R.id.tvPlaceName)
        private val tvAddr = itemView.findViewById<TextView>(R.id.tvAddress)

        fun bind(dto: CallInRoadMapDto, type: ItemType) {
            tvName.text = dto.name
            tvAddr.text = dto.roadAddressName ?: dto.addressName.orEmpty()

            // 이미지 로딩 디버깅
            val url = dto.photoImgUrl
            ivPhoto.load(url) {
                crossfade(true)
                listener(
                    onSuccess = { _: ImageRequest, result: SuccessResult ->
                        Log.v(TAG, "image success url=$url pos=$bindingAdapterPosition size=${result.drawable?.intrinsicWidth}x${result.drawable?.intrinsicHeight}")
                    },
                    onError = { _: ImageRequest, result: ErrorResult ->
                        Log.w(TAG, "image error url=$url pos=$bindingAdapterPosition : ${result.throwable.localizedMessage}")
                    }
                )
            }

            when (type) {
                ItemType.START -> {
                    lineTop.visibility = View.GONE
                    lineBottom.visibility = View.VISIBLE
                    dot.visibility = View.VISIBLE           // 시작: 점 + 아래 선
                    ivArrow.visibility = View.GONE
                }
                ItemType.MIDDLE -> {
                    lineTop.visibility = View.VISIBLE
                    lineBottom.visibility = View.VISIBLE
                    dot.visibility = View.GONE              // 중간: 점 없음, 위/아래 선
                    ivArrow.visibility = View.GONE
                }
                ItemType.END -> {
                    lineTop.visibility = View.VISIBLE
                    lineBottom.visibility = View.GONE
                    dot.visibility = View.GONE              // 끝: 위 선 + 화살표
                    ivArrow.visibility = View.VISIBLE
                }
            }
        }
    }
}
