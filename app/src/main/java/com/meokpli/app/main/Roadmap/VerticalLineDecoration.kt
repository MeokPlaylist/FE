package com.meokpli.app.main.Roadmap

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.main.Roadmap.RoadmapViewFragment.ViewListItem

class VerticalLineDecoration(
    private val lineX: Float,
    private val thickness: Float = 3f
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        this.color = 0x33000000
        this.strokeWidth = thickness
        this.isAntiAlias = true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        val itemCount = adapter.itemCount

        for (i in 0 until itemCount - 1) {
            val vh = parent.findViewHolderForAdapterPosition(i)
            val nextVh = parent.findViewHolderForAdapterPosition(i + 1)

            if (vh == null || nextVh == null) continue

            val item = (adapter as? RoadmapViewFragment.ViewAdapter)?.items?.getOrNull(i)
            val nextItem = (adapter as? RoadmapViewFragment.ViewAdapter)?.items?.getOrNull(i + 1)

            // 둘 다 PlaceEntry일 때만 선 연결
            if (item is ViewListItem.PlaceEntry && nextItem is ViewListItem.PlaceEntry) {
                val currentDay = item.place.dayIndex
                val nextDay = nextItem.place.dayIndex

                if (currentDay == nextDay) {
                    val overlap = 6 * parent.resources.displayMetrics.density
                    val top = vh.itemView.bottom.toFloat() - overlap
                    val bottom = nextVh.itemView.top.toFloat() + overlap
                    c.drawLine(lineX, top, lineX, bottom, paint)
                }
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, 0)
    }
}
