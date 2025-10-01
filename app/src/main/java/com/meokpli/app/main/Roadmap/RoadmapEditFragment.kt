// RoadmapEditFragment.kt
package com.meokpli.app.main.Roadmap

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
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

    private var feedId: Long = 0L

    data class EditItem(
        val seq: Int,
        val doc: KakaoDocument,
        var checked: Boolean = true,   // 포함 여부
        var starred: Boolean = false   // ★ 찜(저장 로직에 영향 X)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        api = Network.roadmapApi(requireContext())
        feedId = requireArguments().getLong("feedId", 0L)
        Log.d(TAG, "onViewCreated feedId=$feedId")
        if (feedId == 0L) {
            Toast.makeText(requireContext(), "feedId가 필요합니다.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // 상단 버튼
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            Log.d(TAG, "click back")
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        view.findViewById<View>(R.id.edit_roadmap).setOnClickListener {
            Log.d(TAG, "click trash (delete checked)")
            deleteChecked()
        }
        view.findViewById<View>(R.id.btnDone).setOnClickListener {
            Log.d(TAG, "click done (save)")
            saveAndExit()
        }

        // 리스트
        rv = view.findViewById(R.id.rvEditRoadmap)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = EditAdapter(
            onToggleCheck = { pos -> toggleCheck(pos) },
            onToggleStar  = { pos -> toggleStar(pos) }
        )
        rv.adapter = adapter

        // 초기 후보 로드
        loadInitial()
    }

    private fun loadInitial() = viewLifecycleOwner.lifecycleScope.launch {
        Log.d(TAG, "loadInitial: pullOutKakao...")
        runCatching { api.pullOutKakao(feedId) }
            .onSuccess { res ->
                Log.d(TAG, "pullOutKakao OK: seqCount=${res.kakaoPlaceInfor.size}")
                val items = mutableListOf<EditItem>()
                res.kakaoPlaceInfor.forEach { (seq, list) ->
                    list.forEachIndexed { idx, doc ->
                        // 첫 후보 기본 포함(checked=true). ★(찜)는 전부 false로 시작.
                        items += EditItem(
                            seq = seq,
                            doc = doc,
                            checked = (idx == 0),
                            starred = false
                        )
                    }
                }
                adapter.submit(items)
                Log.d(TAG, "items.size=${adapter.items.size}")
            }
            .onFailure { e ->
                Log.e(TAG, "pullOutKakao FAILED", e)
                Toast.makeText(requireContext(), "로드맵 후보 불러오기 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteChecked() {
        val before = adapter.items.size
        adapter.removeIf { it.checked }
        val after = adapter.items.size
        Log.d(TAG, "deleteChecked: $before -> $after")
        if (before == after) {
            Toast.makeText(requireContext(), "삭제할 항목이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleCheck(position: Int) {
        if (position !in adapter.items.indices) return
        val it = adapter.items[position]
        it.checked = !it.checked
        Log.d(TAG, "toggleCheck pos=$position checked=${it.checked} seq=${it.seq} place=${it.doc.placeName}")
        adapter.notifyItemChanged(position)
    }

    private fun toggleStar(position: Int) {
        if (position !in adapter.items.indices) return
        val it = adapter.items[position]
        it.starred = !it.starred // 찜 토글 (저장 로직에는 영향 없음)
        Log.d(TAG, "toggleStar pos=$position starred=${it.starred} seq=${it.seq} place=${it.doc.placeName}")
        adapter.notifyItemChanged(position)
    }

    private fun saveAndExit() = viewLifecycleOwner.lifecycleScope.launch {
        // 체크된(포함) 항목만 seq별 대표 1개를 고르는 게 아니라,
        // 요구사항상 '별은 찜일 뿐'이므로 **대표 개념 없이** seq마다 '체크된 항목 중 첫 번째'만 저장 대상으로 삼음.
        val selected = adapter.items.filter { it.checked }
        Log.d(TAG, "saveAndExit selected.count=${selected.size}")
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "선택된 장소가 없습니다.", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val bySeq = selected.groupBy { it.seq }
        val map = mutableMapOf<Int, KakaoDocument>()
        bySeq.forEach { (seq, list) ->
            val chosen = list.firstOrNull()
            if (chosen != null) map[seq] = chosen.doc
        }
        Log.d(TAG, "saveAndExit map.size=${map.size} feedId=$feedId")

        if (map.isEmpty()) {
            Toast.makeText(requireContext(), "선택된 장소가 없습니다.", Toast.LENGTH_SHORT).show()
            return@launch
        }

        runCatching {
            api.saveRoadMap(SaveRoadMapPlaceRequest(feedId = feedId, saveRoadMapPlaceInfor = map))
        }.onSuccess { res ->
            if (res.isSuccessful) {
                Log.i(TAG, "saveRoadMap OK(${res.code()}) → navigate Home")
                findNavController().navigate(R.id.homeFragment)
                Toast.makeText(requireContext(), "로드맵이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "saveRoadMap FAIL code=${res.code()} message=${res.errorBody()?.string()}")
                Toast.makeText(requireContext(),
                    "저장 실패(${res.code()}): 서버 응답을 확인하세요.", Toast.LENGTH_LONG).show()
            }
        }.onFailure { e ->
            Log.e(TAG, "saveRoadMap EXCEPTION", e)
            Toast.makeText(requireContext(), "저장 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // -------- Adapter --------
    inner class EditAdapter(
        private val onToggleCheck: (Int) -> Unit,
        private val onToggleStar: (Int) -> Unit
    ) : RecyclerView.Adapter<EditAdapter.VH>() {

        val items = mutableListOf<EditItem>()

        fun submit(newItems: List<EditItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun removeIf(predicate: (EditItem) -> Boolean) {
            val it = items.iterator()
            var changed = false
            while (it.hasNext()) {
                if (predicate(it.next())) { it.remove(); changed = true }
            }
            if (changed) notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_roadmap_edit, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivCheck = itemView.findViewById<ImageView>(R.id.ivCheck)
            private val ivStar  = itemView.findViewById<ImageView>(R.id.ivStar)
            private val ivPhoto = itemView.findViewById<ImageView>(R.id.ivPhoto)
            private val tvName  = itemView.findViewById<TextView>(R.id.tvPlaceName)
            private val tvAddr  = itemView.findViewById<TextView>(R.id.tvAddress)

            fun bind(it: EditItem) {
                // 체크
                ivCheck.setImageResource(
                    if (it.checked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                )
                ivCheck.setOnClickListener { onToggleCheck(bindingAdapterPosition) }

                // 별(찜)
                ivStar.setImageResource(
                    if (it.starred) R.drawable.ic_star_checked else R.drawable.ic_star_unchecked
                )
                ivStar.setOnClickListener { onToggleStar(bindingAdapterPosition) }

                // 사진 썸네일: 업로드 직후 로컬 썸네일 전달 구조가 없으니 일단 서버 제공 이미지가 있으면 사용
                // (필요하면 Bundle로 업로드 썸네일 리스트를 넘겨와서 seq 매칭해 표시 가능)
                ivPhoto.setImageResource(R.drawable.ic_placeholder)

                tvName.text = it.doc.placeName
                tvAddr.text = it.doc.roadAddressName ?: it.doc.addressName ?: ""

                itemView.setOnClickListener {
                    // 행 클릭 시 체크 토글(편의)
                    onToggleCheck(bindingAdapterPosition)
                }
            }
        }
    }
}
