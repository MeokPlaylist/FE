package com.meokpli.app.main.Roadmap

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch

class RoadmapEditFragment : Fragment(R.layout.fragment_roadmap_edit) {

    private lateinit var api: RoadmapApi
    private lateinit var rv: RecyclerView
    private lateinit var adapter: EditAdapter
    private lateinit var etTitle: EditText

    private val vm: RoadmapEditViewModel by viewModels() // ✅ ViewModel 연결
    private var feedId: Long = 0L

    data class EditItem(
        val roadMapPlaceId: Long,
        val photoUrl: String,
        val candidates: List<PlaceCandidate>,
        var selectedPlaceId: Long?,
        var expanded: Boolean = false,
        var customName: String? = null,
        var customAddress: String? = null,
        var isSelected: Boolean = false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        api = Network.roadmapApi(requireContext())
        feedId = requireArguments().getLong("feedId", 0L)
        etTitle = view.findViewById(R.id.etTripTitle)

        rv = view.findViewById(R.id.rvEditRoadmap)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = EditAdapter(::toggleExpand, ::onCandidateSelect)
        rv.adapter = adapter

        // ✅ ViewModel에서 상태 복원
        etTitle.setText(vm.tripTitle)
        if (!vm.editItems.value.isNullOrEmpty()) {
            adapter.submit(vm.editItems.value!!)
        } else {
            loadCandidates()
        }

        // 완료 버튼
        view.findViewById<View>(R.id.btnDone).setOnClickListener { saveRoadMap() }

        // 빈 영역 터치 시 키보드 닫기
        view.setOnTouchListener { v, event ->
            hideKeyboard(v)
            false
        }
        rv.setOnTouchListener { v, event ->
            hideKeyboard(v)
            v.clearFocus()
            false
        }

        view.findViewById<View>(R.id.edit_roadmap)?.setOnClickListener {
            val removedCount = adapter.removeSelectedItems()
            if (removedCount > 0) {
                Toast.makeText(requireContext(), "${removedCount}개 항목이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                vm.editItems.value = adapter.items
            } else {
                Toast.makeText(requireContext(), "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCandidates() = viewLifecycleOwner.lifecycleScope.launch {
        val list = runCatching { api.createRoadmap(feedId) }.getOrNull().orEmpty()
        if (list.isEmpty()) return@launch

        val grouped = list.groupBy { it.dayIndex?.toInt() }
        val items = mutableListOf<EditListItem>()

        grouped.toSortedMap(compareBy<Int?> { it ?: Int.MAX_VALUE })
            .forEach { (day, dtos) ->
                val safeDay = day ?: 0
                val firstDate = dtos.mapNotNull { it.dateTime }
                    .minOrNull()
                    ?.substring(0, 10)
                    ?.replace("-", ". ")
                items.add(EditListItem.DayHeaderLabel(safeDay, firstDate))
                dtos.forEach { dto ->
                    items.add(
                        EditListItem.EditEntry(
                            EditItem(
                                roadMapPlaceId = dto.roadMapPlaceId,
                                photoUrl = dto.photoUrl,
                                candidates = dto.candidates,
                                selectedPlaceId = dto.candidates.firstOrNull()?.placeId
                            )
                        )
                    )
                }
            }

        adapter.submit(items)
        vm.editItems.value = items // ✅ ViewModel에 저장
    }

    private fun saveRoadMap() = viewLifecycleOwner.lifecycleScope.launch {
        // ✅ 제목 저장
        vm.tripTitle = etTitle.text.toString().trim()

        // ✅ adapter의 남은 아이템만 저장용 리스트로 변환
        val remainingPlaces = adapter.items
            .filterIsInstance<EditListItem.EditEntry>() // 장소 항목만 추출
            .map { entry ->
                val item = entry.item
                SaveRoadMapPlaceItem(
                    roadMapPlaceId = item.roadMapPlaceId,
                    selectedPlaceId = item.selectedPlaceId,
                    customPlaceName = item.customName,
                    customAddress = item.customAddress
                )
            }

        if (remainingPlaces.isEmpty()) {
            Toast.makeText(requireContext(), "저장할 장소가 없습니다.", Toast.LENGTH_SHORT).show()
            return@launch
        }

        // ✅ 제목 결정 (입력값이 없으면 "여행")
        val finalTitle = if (vm.tripTitle.isNotBlank()) vm.tripTitle else "여행"

        // ✅ 요청 DTO 생성
        val body = SaveRoadMapPlaceRequest(
            feedId = feedId,
            title = finalTitle,
            places = remainingPlaces
        )

        // ✅ API 호출 (create → save로 변경)
        runCatching {
            api.saveRoadMap(body)
        }.onSuccess { res ->
            if (res.isSuccessful) {
                Toast.makeText(requireContext(), "로드맵 저장 완료!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
            } else {
                Toast.makeText(requireContext(), "서버 오류 (${res.code()})", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            Toast.makeText(requireContext(), "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun toggleExpand(pos: Int) {
        val item = adapter.items.getOrNull(pos)
        if (item is EditListItem.EditEntry) {
            item.item.expanded = !item.item.expanded
            adapter.notifyItemChanged(pos)
            vm.editItems.value = adapter.items // ✅ 상태 갱신
        }
    }

    private fun onCandidateSelect(pos: Int, placeId: Long) {
        val item = adapter.items.getOrNull(pos)
        if (item is EditListItem.EditEntry) {
            item.item.selectedPlaceId = placeId
            item.item.expanded = false
            adapter.notifyItemChanged(pos)
            vm.editItems.value = adapter.items // ✅ 상태 갱신
        }
    }

    // ---------------- Adapter ----------------
    inner class EditAdapter(
        private val onExpand: (Int) -> Unit,
        private val onSelect: (Int, Long) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_DAY_HEADER = 0
        private val TYPE_EDIT_ITEM = 1
        val items = mutableListOf<EditListItem>()

        fun submit(newItems: List<EditListItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun removeSelectedItems(): Int {
            val beforeSize = items.size

            // 1️⃣ 체크된 장소 삭제
            val filtered = items.filterNot { it is EditListItem.EditEntry && it.item.isSelected }

            // 2️⃣ 헤더별로 장소가 남아있는지 확인
            val daysWithPlaces = mutableSetOf<Int>()
            filtered.forEach { item ->
                if (item is EditListItem.EditEntry) {
                    // 헤더는 바로 앞 item.DayHeaderLabel.day와 매칭되므로, 역으로 찾을 수 있게
                    val idx = filtered.indexOf(item)
                    // 앞쪽에서 가장 가까운 DayHeaderLabel 찾기
                    for (i in idx downTo 0) {
                        val prev = filtered[i]
                        if (prev is EditListItem.DayHeaderLabel) {
                            daysWithPlaces.add(prev.day)
                            break
                        }
                    }
                }
            }

            // 3️⃣ 장소가 없는 day의 헤더는 제거
            val cleaned = filtered.filterNot { it is EditListItem.DayHeaderLabel && it.day !in daysWithPlaces }

            items.clear()
            items.addAll(cleaned)
            notifyDataSetChanged()
            return beforeSize - items.size
        }


        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is EditListItem.DayHeaderLabel -> TYPE_DAY_HEADER
            is EditListItem.EditEntry -> TYPE_EDIT_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_DAY_HEADER -> DayHeaderVH(layoutInflater.inflate(R.layout.item_day_header, parent, false))
                else -> EditVH(layoutInflater.inflate(R.layout.item_roadmap_edit, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is EditListItem.DayHeaderLabel -> (holder as DayHeaderVH).bind(item)
                is EditListItem.EditEntry -> (holder as EditVH).bind(item.item)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class DayHeaderVH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvDay = v.findViewById<TextView>(R.id.tvDayHeader)
            fun bind(item: EditListItem.DayHeaderLabel) {
                tvDay.text = if (item.date != null)
                    "${item.day}일차 (${item.date})"
                else "${item.day}일차"
            }
        }

        inner class EditVH(v: View) : RecyclerView.ViewHolder(v) {
            private val ivCheck = v.findViewById<ImageView>(R.id.ivCheck)
            private val ivPhoto = v.findViewById<ImageView>(R.id.ivPhoto)
            private val etName = v.findViewById<TextView>(R.id.etPlaceName)
            private val etAddr = v.findViewById<TextView>(R.id.etAddress)
            private val tvMore = v.findViewById<TextView>(R.id.tvMoreCandidates)
            private val rvCand = v.findViewById<RecyclerView>(R.id.rvCandidates)
            private val card = v.findViewById<MaterialCardView>(R.id.card)

            fun bind(item: EditItem) {
                ivPhoto.load(item.photoUrl)
                val selected = item.candidates.find { it.placeId == item.selectedPlaceId }
                etName.text = selected?.placeName ?: item.customName ?: "가게 이름을 입력해주세요."
                etAddr.text = selected?.address ?: item.customAddress ?: "가게 주소를 입력해주세요."

                etName.setOnFocusChangeListener { v, hasFocus ->
                    if (!hasFocus) {
                        val newText = etName.text.toString().trim()
                        if (newText.isNotBlank()) item.customName = newText
                        hideKeyboard(v)
                        vm.editItems.value = adapter.items
                    }
                }
                etAddr.setOnFocusChangeListener { v, hasFocus ->
                    if (!hasFocus) {
                        val newText = etAddr.text.toString().trim()
                        if (newText.isNotBlank()) item.customAddress = newText
                        hideKeyboard(v)
                        vm.editItems.value = adapter.items
                    }
                }

                ivCheck.setImageResource(
                    if (item.isSelected) R.drawable.ic_checkbox_checked
                    else R.drawable.ic_checkbox_unchecked
                )
                ivCheck.setOnClickListener {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(bindingAdapterPosition)
                    vm.editItems.value = adapter.items
                }

                card.setOnClickListener {
                    hideKeyboard(it)
                    it.clearFocus()
                    onExpand(bindingAdapterPosition)
                }

                val moreCount = item.candidates.size - 1
                tvMore.visibility = if (moreCount > 0 && !item.expanded) View.VISIBLE else View.GONE
                tvMore.text = "+${moreCount}개 더보기"
                tvMore.setOnClickListener { onExpand(bindingAdapterPosition) }

                if (item.expanded) {
                    rvCand.visibility = View.VISIBLE
                    rvCand.layoutManager = LinearLayoutManager(itemView.context)
                    rvCand.adapter = CandidateAdapter(item.candidates, item.selectedPlaceId) { placeId ->
                        onSelect(bindingAdapterPosition, placeId)
                    }
                } else rvCand.visibility = View.GONE
            }
        }
    }

    sealed class EditListItem {
        data class DayHeaderLabel(val day: Int, val date: String?) : EditListItem()
        data class EditEntry(val item: EditItem, var isSelected: Boolean = false) : EditListItem()
    }

    inner class CandidateAdapter(
        private val list: List<PlaceCandidate>,
        private val selectedId: Long?,
        private val onSelect: (Long) -> Unit
    ) : RecyclerView.Adapter<CandidateAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val name = itemView.findViewById<TextView>(R.id.tvCandidateName)
            private val addr = itemView.findViewById<TextView>(R.id.tvCandidateAddr)
            fun bind(item: PlaceCandidate) {
                name.text = item.placeName
                addr.text = item.address
                itemView.alpha = if (item.placeId == selectedId) 1f else 0.6f
                itemView.setOnClickListener { onSelect(item.placeId) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_candidate_row, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(list[position])
        override fun getItemCount(): Int = list.size
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
