package com.meokpli.app.main.Roadmap

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.meokpli.app.R
import com.meokpli.app.databinding.FragmentRoadmapViewBinding
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch

class RoadmapViewFragment : Fragment() {

    companion object { private const val TAG = "RoadmapViewFragment" }

    private var _binding: FragmentRoadmapViewBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy { RoadmapAdapter(onPhotoClick = ::openPhoto) }
    private val api by lazy { Network.roadmapApi(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentRoadmapViewBinding.inflate(inflater, container, false)
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

        viewLifecycleOwner.lifecycleScope.launch {
            val t0 = SystemClock.elapsedRealtime()
            Log.d(TAG, "getRoadmap() start")
            runCatching { api.getRoadmap(feedId) }         // 저장된 로드맵 조회
                .onSuccess { res ->
                    val dt = SystemClock.elapsedRealtime() - t0
                    val count = res.callInRoadMapDtoList.size
                    Log.i(TAG, "getRoadmap() ok in ${dt}ms, items=$count")

                    adapter.items = res.callInRoadMapDtoList
                    adapter.notifyDataSetChanged()

                    if (count == 0) {
                        Toast.makeText(requireContext(), "저장된 로드맵이 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val first = res.callInRoadMapDtoList.first()
                        Log.v(TAG, "first item name='${first.name}', photo='${first.photoImgUrl}'")
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "getRoadmap() fail", e)
                    Toast.makeText(requireContext(), "로드맵 불러오기 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
            putString("photoUrl", dto.photoImgUrl)
        }
        findNavController().navigate(R.id.roadmapPhotoDialog, args)
    }
}
