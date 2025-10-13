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
        var checked: Boolean = false,   // 포함 여부
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
    private fun toKakaoDocFromSaved(index: Int, s: CallInRoadMapDto): KakaoDocument {
        val tmpId = "saved-${index + 1}" // 화면용 임시 ID (저장 금지 가드가 있으므로 서버로 안 보냄)
        return KakaoDocument(
            id = tmpId,
            placeName = s.name ?: "",
            addressName = s.addressName,
            roadAddressName = s.roadAddressName,
            placeUrl = null,                      // 저장본에 없음
            phone = s.phone,                      // nullable OK
            categoryGroupCode = null,             // 저장본에 없음
            categoryGroupName = s.kakaoCategoryName
        )
    }

    private fun loadInitial() = viewLifecycleOwner.lifecycleScope.launch {
        Log.d(TAG, "loadInitial: 저장본 확인 후 필요 시에만 pullOutKakao")
        val api = this@RoadmapEditFragment.api

        // 1) 저장본 먼저 조회
        val saved = runCatching { api.getRoadmap(feedId) }.getOrNull()
        val savedList = saved?.callInRoadMapDtoList.orEmpty()
        Log.d(TAG, "getRoadmap: savedCount=${savedList.size}")

        // 2) 저장본이 있으면 후보 추출 생략, 없으면 1회만 후보 추출
        val candidates: Map<Int, List<KakaoDocument>> = if (savedList.isNotEmpty()) {
            emptyMap()
        } else {
            runCatching { api.pullOutKakao(feedId).kakaoPlaceInfor }
                .getOrElse {
                    Log.e(TAG, "pullOutKakao 실패", it)
                    emptyMap()
                }
        }
        Log.d(TAG, "candidates(seqCount)=${candidates.size}")

        val items = mutableListOf<EditItem>()

        if (savedList.isNotEmpty()) {
            // 저장본을 화면에 그대로 보여준다(편집은 가능하게 두되 '저장'은 비활성화)
            savedList.forEachIndexed { idx, s ->
                val doc = toKakaoDocFromSaved(idx, s)
                items += EditItem(
                    seq = (idx + 1),
                    doc = doc,
                    checked = false,
                    starred = false
                )
            }
            adapter.submit(items)
            // 저장본만 있을 땐 저장 비활성화(서버가 placeId를 내려주기 전까지)
            view?.findViewById<View>(R.id.btnDone)?.isEnabled = false
            Toast.makeText(requireContext(), "저장본입니다. 후보 추출 후 수정/저장 가능합니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 후보가 있을 때만 저장 가능
            candidates.forEach { (seq, docs) ->
                docs.forEachIndexed { idx, doc ->
                    val checkedDefault = (idx == 0) // 각 seq의 첫 후보만 기본 선택(필요 시 정책 조정)
                    items += EditItem(seq = seq, doc = doc, checked = checkedDefault, starred = false)
                }
            }
            adapter.submit(items)
            view?.findViewById<View>(R.id.btnDone)?.isEnabled = items.isNotEmpty()
        }

        Log.i(TAG, "로드맵 편집 초기화 완료: items=${adapter.items.size}, checked=${adapter.items.count { it.checked }}")
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
        // 체크 안 된 항목만 저장 대상
        val toSave = adapter.items.filter { !it.checked }
        Log.d(TAG, "saveAndExit toSave.count=${toSave.size}")

        if (toSave.isEmpty()) {
            Toast.makeText(requireContext(), "저장할 장소가 없습니다.", Toast.LENGTH_SHORT).show()
            return@launch
        }

        // seq별 첫 번째만 저장 (백엔드가 seq->단일 장소 요구 시)
        val bySeq = toSave.groupBy { it.seq }
        val map = mutableMapOf<Int, KakaoDocument>()
        bySeq.forEach { (seq, list) ->
            list.firstOrNull()?.let { map[seq] = it.doc }
        }
        Log.d(TAG, "saveAndExit map.size=${map.size} feedId=$feedId")

        if (map.isEmpty()) {
            Toast.makeText(requireContext(), "저장할 장소가 없습니다.", Toast.LENGTH_SHORT).show()
            return@launch
        }

        val api = Network.roadmapApi(requireContext())
        runCatching {
            api.saveRoadMap(SaveRoadMapPlaceRequest(feedId = feedId, saveRoadMapPlaceInfor = map))
        }.onSuccess { res ->
            if (res.isSuccessful) {
                Log.i(TAG, "saveRoadMap OK(${res.code()})")
                Toast.makeText(requireContext(), "로드맵이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                // 필요에 맞게 한 가지 선택
                // findNavController().navigate(R.id.homeFragment)
                findNavController().popBackStack()
            } else {
                Log.w(TAG, "saveRoadMap FAIL code=${res.code()} message=${res.errorBody()?.string()}")
                Toast.makeText(requireContext(), "저장 실패(${res.code()})", Toast.LENGTH_LONG).show()
            }
        }.onFailure { e ->
            Log.e(TAG, "saveRoadMap EXCEPTION", e)
            Toast.makeText(requireContext(), "저장 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    private fun removeSelected() {
        // ✅ 체크된 아이템들만 제거
        val before = adapter.items.size
        val removed = adapter.items.removeAll { it.checked }
        if (removed) {
            adapter.notifyDataSetChanged()
            Toast.makeText(
                requireContext(),
                "선택한 ${before - adapter.items.size}개 항목을 삭제했어요. 저장을 눌러 반영하세요.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(requireContext(), "삭제할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show()
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
