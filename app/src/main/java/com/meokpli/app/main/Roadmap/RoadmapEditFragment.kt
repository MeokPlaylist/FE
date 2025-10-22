package com.meokpli.app.main.Roadmap

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch

class RoadmapEditFragment : Fragment(R.layout.fragment_roadmap_edit) {

    private val TAG = "RoadmapEdit"

    private lateinit var api: RoadmapApi
    private lateinit var rv: RecyclerView
    private lateinit var adapter: EditAdapter
    private lateinit var etTitle: EditText

    private var feedId: Long = 0L

    data class EditItem(
        val roadMapPlaceId: Long,
        val photoUrl: String,
        val candidates: List<PlaceCandidate>,
        var selectedPlaceId: Long?,
        var expanded: Boolean = false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        api = Network.roadmapApi(requireContext())
        feedId = requireArguments().getLong("feedId", 0L)
        etTitle = view.findViewById(R.id.etTripTitle)

        rv = view.findViewById(R.id.rvEditRoadmap)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = EditAdapter(::toggleExpand, ::onCandidateSelect)
        rv.adapter = adapter

        view.findViewById<View>(R.id.btnDone).setOnClickListener { saveRoadMap() }

        loadCandidates()
    }

    private fun loadCandidates() = viewLifecycleOwner.lifecycleScope.launch {
        val list = runCatching { api.createRoadmap(feedId) }.getOrNull().orEmpty()
        val items = list.map { dto ->
            EditItem(
                roadMapPlaceId = dto.roadMapPlaceId,
                photoUrl = dto.photoUrl,
                candidates = dto.candidates,
                selectedPlaceId = dto.candidates.firstOrNull()?.placeId
            )
        }
        adapter.submit(items)
    }

    private fun saveRoadMap() = viewLifecycleOwner.lifecycleScope.launch {
        val title = etTitle.text.toString().ifBlank { "제목 없음" }
        val map = adapter.items.associate { it.roadMapPlaceId to (it.selectedPlaceId ?: 0L) }

        val body = SaveRoadMapPlaceRequest(feedId, title = title, saveRoadMapPlaceInfor = map)
        val res = runCatching { api.saveRoadMap(body) }.getOrElse {
            Toast.makeText(requireContext(), "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            return@launch
        }

        if (res.isSuccessful) {
            Toast.makeText(requireContext(), "저장 완료!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.homeFragment)
        } else {
            Toast.makeText(requireContext(), "서버 오류(${res.code()})", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleExpand(pos: Int) {
        adapter.items[pos].expanded = !adapter.items[pos].expanded
        adapter.notifyItemChanged(pos)
    }

    private fun onCandidateSelect(pos: Int, placeId: Long) {
        adapter.items[pos].selectedPlaceId = placeId
        adapter.items[pos].expanded = false
        adapter.notifyItemChanged(pos)
    }

    // ---------------- Adapter ----------------
    inner class EditAdapter(
        private val onExpand: (Int) -> Unit,
        private val onSelect: (Int, Long) -> Unit
    ) : RecyclerView.Adapter<EditAdapter.VH>() {

        val items = mutableListOf<EditItem>()

        fun submit(newItems: List<EditItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_roadmap_edit, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val ivPhoto = v.findViewById<ImageView>(R.id.ivPhoto)
            private val tvName = v.findViewById<TextView>(R.id.tvPlaceName)
            private val tvAddr = v.findViewById<TextView>(R.id.tvAddress)
            private val tvMore = v.findViewById<TextView>(R.id.tvMoreCandidates)
            private val rvCand = v.findViewById<RecyclerView>(R.id.rvCandidates)

            fun bind(item: EditItem) {
                ivPhoto.load(item.photoUrl)
                val selected = item.candidates.find { it.placeId == item.selectedPlaceId }
                tvName.text = selected?.placeName ?: "선택 안 함"
                tvAddr.text = selected?.address ?: ""

                val moreCount = item.candidates.size - 1
                tvMore.visibility = if (moreCount > 0 && !item.expanded) View.VISIBLE else View.GONE
                tvMore.text = "+${moreCount}개 더보기"

                tvMore.setOnClickListener { onExpand(bindingAdapterPosition) }
                itemView.setOnClickListener { onExpand(bindingAdapterPosition) }

                // 후보 리스트 표시
                if (item.expanded) {
                    rvCand.visibility = View.VISIBLE
                    rvCand.layoutManager = LinearLayoutManager(
                        itemView.context, LinearLayoutManager.VERTICAL, false
                    )
                    rvCand.adapter = CandidateAdapter(item.candidates, item.selectedPlaceId) { placeId ->
                        onSelect(bindingAdapterPosition, placeId)
                    }
                } else {
                    rvCand.visibility = View.GONE
                }
            }
        }
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
}