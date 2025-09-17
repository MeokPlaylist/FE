package com.meokpli.app.main.Roadmap

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.databinding.FragmentRoadmapPreviewBinding
import kotlinx.coroutines.launch

class RoadmapPreviewFragment : Fragment() {

    companion object { private const val TAG = "RoadmapPreviewFragment" }

    private var _binding: FragmentRoadmapPreviewBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy { RoadmapAdapter(onPhotoClick = ::openPhoto) }

    private val api by lazy { Network.roadmapApi(requireContext()) }

    /** seq -> KakaoDocument : pullOut 결과에서 '첫 후보'로 임시 선택한 매핑 */
    private val provisionalMap = linkedMapOf<Int, KakaoDocument>()



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentRoadmapPreviewBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView()")
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        Log.d(TAG, "onViewCreated()")

        binding.rvRoadmap.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoadmap.adapter = adapter

        val feedId = requireArguments().getLong("feedId")
        Log.i(TAG, "feedId=$feedId")

        // 1) 자동 생성(간이 미리보기): pullOut → 각 seq의 첫 후보 채택
        viewLifecycleOwner.lifecycleScope.launch {
            val t0 = SystemClock.elapsedRealtime()
            Log.d(TAG, "pullOutKakao() start")
            runCatching { api.pullOutKakao(feedId) }
                .onSuccess { res ->
                    Log.d(TAG, "pullOutKakao() ok in ${SystemClock.elapsedRealtime() - t0}ms")
                    provisionalMap.clear()

                    val preview = mutableListOf<CallInRoadMapDto>()
                    val sortedSeqs = res.kakaoPlaceInfor.keys.sorted()
                    sortedSeqs.forEach { seq ->
                        val cands = res.kakaoPlaceInfor[seq].orEmpty()
                        if (cands.isNotEmpty()) {
                            val pick = cands.first()
                            provisionalMap[seq] = pick

                            preview += CallInRoadMapDto(
                                name = pick.placeName,
                                addressName = pick.roadAddressName ?: pick.addressName,
                                roadAddressName = null,
                                phone = pick.phone,
                                kakaoCategoryName = pick.categoryGroupName,
                                photoImgUrl = null   // 업로드 직후엔 서버 사진 URL이 없을 수 있음
                            )
                        } else {
                            Log.w(TAG, "no candidates for seq=$seq")
                        }
                    }

                    adapter.items = preview
                    adapter.notifyDataSetChanged()
                    Log.i(TAG, "preview list size=${preview.size}, provisionalMap size=${provisionalMap.size}")

                    if (preview.isEmpty()) {
                        Toast.makeText(requireContext(), "자동 생성할 후보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "pullOutKakao() fail in ${SystemClock.elapsedRealtime() - t0}ms", e)
                    Toast.makeText(requireContext(), "자동 생성 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        // 2) 완료(저장) → save 후 보기 전용 화면으로 이동
        binding.submitButton.setOnClickListener {
            if (provisionalMap.isEmpty()) {
                Toast.makeText(requireContext(), "저장할 항목이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 버튼 상태 잠금
            val oldText = binding.submitButton.text
            binding.submitButton.isEnabled = false
            binding.submitButton.text = getString(R.string.saving) /* values/strings.xml에 "saving" 추가 권장 */

            viewLifecycleOwner.lifecycleScope.launch {
                val body = SaveRoadMapPlaceRequest(feedId, provisionalMap)
                val t0 = SystemClock.elapsedRealtime()
                Log.d(TAG, "saveRoadMap() start; bodySize=${provisionalMap.size}")

                runCatching { api.saveRoadMap(body) }
                    .onSuccess { resp ->
                        val dt = SystemClock.elapsedRealtime() - t0
                        Log.i(TAG, "saveRoadMap() response code=${resp.code()} in ${dt}ms")
                        if (resp.isSuccessful) {
                            Toast.makeText(requireContext(), "로드맵이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            // 저장 후 보기 전용 화면으로 이동
                            findNavController().navigate(
                                R.id.action_roadmapPreview_to_roadmapView,
                                bundleOf("feedId" to feedId)
                            )
                        } else {
                            Toast.makeText(requireContext(), "저장 실패(${resp.code()})", Toast.LENGTH_LONG).show()
                            // 실패 복구
                            binding.submitButton.isEnabled = true
                            binding.submitButton.text = oldText
                        }
                    }
                    .onFailure { e ->
                        Log.e(TAG, "saveRoadMap() fail", e)
                        Toast.makeText(requireContext(), "저장 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        // 실패 복구
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = oldText
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView()")
        _binding = null
    }

    private fun openPhoto(dto: CallInRoadMapDto) {
        val args = Bundle().apply {
            putString("title", dto.name)
            putString("address", dto.roadAddressName ?: dto.addressName ?: "")
            putString("photoUrl", dto.photoImgUrl) // content://, file://, http(s) 모두 가능
        }
        findNavController().navigate(R.id.roadmapPhotoDialog, args)
    }
}
