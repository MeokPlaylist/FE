package com.meokpli.app.main.Roadmap

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.databinding.FragmentRoadmapViewBinding
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class RoadmapViewFragment : Fragment() {

    private var _binding: FragmentRoadmapViewBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: RoadmapApi
    private lateinit var adapter: ViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentRoadmapViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val ctx = context ?: return
        api = Network.roadmapApi(ctx)
        adapter = ViewAdapter()
        binding.rvRoadmap.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoadmap.adapter = adapter

        // 세로라인 ItemDecoration 추가 (X좌표는 화면 비율에 맞게)
        val lineX = resources.displayMetrics.widthPixels * 0.2652f  // 사진과 카드 사이 중앙
        binding.rvRoadmap.addItemDecoration(VerticalLineDecoration(lineX))

        val feedId = requireArguments().getLong("feedId")

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        loadRoadMap(feedId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRoadMap(feedId: Long) = viewLifecycleOwner.lifecycleScope.launch {
        runCatching { api.loadRoadMap(feedId) }
            .onSuccess { res ->
                if (!isAdded || view == null) return@onSuccess

                val items = mutableListOf<ViewListItem>()

                //  문자열 파싱 (예: "2025-10-25T20:44:00")
                val baseDate = try {
                    res.firstDayAndTime?.let { LocalDateTime.parse(it) }?.toLocalDate()
                } catch (e: Exception) {
                    Log.e("RoadmapViewFragment", "날짜 파싱 실패: ${e.message}")
                    null
                }

                val formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd")

                val grouped = res.loadRoadMapPlacesList.orEmpty().groupBy { it.dayIndex ?: 1 }

                grouped.toSortedMap().forEach { (day, places) ->
                    val safeDay = day ?: 1
                    val formattedDate = baseDate
                        ?.plusDays((safeDay - 1).toLong())
                        ?.format(formatter)

                    items.add(ViewListItem.DayHeaderLabel(safeDay, formattedDate))
                    places.sortedBy { it.orderIndex ?: 0 }.forEach { place ->
                        items.add(ViewListItem.PlaceEntry(place))
                    }
                }

                if (_binding != null && isAdded) {
                    adapter.submit(items)
                    binding.tvTripTitle.text = res.title ?: "로드맵"
                }
            }
            .onFailure {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "로드맵 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("RoadmapViewFragment", "Fragment not attached, skip Toast. cause=${it.message}")
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ================== Adapter ==================
    inner class ViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_DAY_HEADER = 0
        private val TYPE_PLACE = 1

        val items = mutableListOf<ViewListItem>()

        fun submit(newItems: List<ViewListItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is ViewListItem.DayHeaderLabel -> TYPE_DAY_HEADER
            is ViewListItem.PlaceEntry -> TYPE_PLACE
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_DAY_HEADER -> {
                    val v = layoutInflater.inflate(R.layout.item_day_header, parent, false)
                    DayHeaderVH(v)
                }
                else -> {
                    val v = layoutInflater.inflate(R.layout.item_roadmap_timeline, parent, false)
                    PlaceVH(v)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ViewListItem.DayHeaderLabel -> (holder as DayHeaderVH).bind(item)
                is ViewListItem.PlaceEntry -> (holder as PlaceVH).bind(item.place)
            }
        }

        override fun getItemCount() = items.size

        inner class DayHeaderVH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvDay = v.findViewById<TextView>(R.id.tvDayHeader)
            fun bind(item: ViewListItem.DayHeaderLabel) {
                if (item.date != null)
                    tvDay.text = "${item.day}일차 (${item.date})"
                else
                    tvDay.text = "${item.day}일차"
            }
        }

        inner class PlaceVH(v: View) : RecyclerView.ViewHolder(v) {
            private val ivPhoto = v.findViewById<ImageView>(R.id.ivPhoto)
            private val tvName = v.findViewById<TextView>(R.id.tvPlaceName)
            private val tvAddr = v.findViewById<TextView>(R.id.tvAddress)
            private val tvPhone = v.findViewById<TextView>(R.id.tvPhone)
            private val card = v.findViewById<MaterialCardView>(R.id.card)
            private val lineTop = v.findViewById<View>(R.id.lineTop)
            private val lineBottom = v.findViewById<View>(R.id.lineBottom)
            private val ivArrow = v.findViewById<ImageView>(R.id.ivArrow)
            private val dot = v.findViewById<View>(R.id.dot)

            fun bind(p: LoadRoadMapPlace) {
                ivPhoto.load(p.presignedGetPhotoUrl)
                tvName.text = p.name
                tvAddr.text = p.address

                if (!p.phone.isNullOrBlank()) {
                    tvPhone.text = "전화번호: ${p.phone}"
                    tvPhone.visibility = View.VISIBLE
                } else tvPhone.visibility = View.GONE

                val pos = bindingAdapterPosition
                val items = adapter.items

                val currentDay = (items[pos] as? ViewListItem.PlaceEntry)?.place?.dayIndex
                val prevDay = (items.getOrNull(pos - 1) as? ViewListItem.PlaceEntry)?.place?.dayIndex
                val nextDay = (items.getOrNull(pos + 1) as? ViewListItem.PlaceEntry)?.place?.dayIndex

                lineTop.visibility = View.GONE
                lineBottom.visibility = View.GONE
                dot.visibility = View.GONE
                ivArrow.visibility = View.GONE

                // 첫 장소
                if (prevDay != currentDay && nextDay == currentDay) {
                    dot.visibility = View.VISIBLE
                    lineBottom.visibility = View.VISIBLE
                }
                // 중간 장소
                else if (prevDay == currentDay && nextDay == currentDay) {
                    lineTop.visibility = View.VISIBLE
                    lineBottom.visibility = View.VISIBLE
                }
                // 마지막 장소
                else if (prevDay == currentDay && nextDay != currentDay) {
                    lineTop.visibility = View.VISIBLE
                    dot.visibility = View.VISIBLE
                    ivArrow.visibility = View.VISIBLE
                }
                // 하루에 하나만 있을 때
                else if (prevDay != currentDay && nextDay != currentDay) {
                    dot.visibility = View.VISIBLE
                }
            }
        }
    }

    sealed class ViewListItem {
        data class DayHeaderLabel(val day: Int, val date: String?) : ViewListItem()
        data class PlaceEntry(val place: LoadRoadMapPlace) : ViewListItem()
    }
}
