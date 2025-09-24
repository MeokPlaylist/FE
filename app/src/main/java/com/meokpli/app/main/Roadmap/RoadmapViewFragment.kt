package com.meokpli.app.main.Roadmap

import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.meokpli.app.R
import com.meokpli.app.databinding.FragmentRoadmapViewBinding
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch
import android.graphics.Color

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

        val writerNickname = requireArguments().getString("writerNickname")
        val isMine = requireArguments().getBoolean("isMine", false)

        if (isMine) {
            binding.btnMore.setOnClickListener { anchor ->
                showRoadmapPopup(anchor)
            }


        } else {
            binding.btnMore.visibility = if (isMine) View.VISIBLE else View.GONE

        }
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

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
    private fun showRoadmapPopup(anchor: View) {
        val v = LayoutInflater.from(anchor.context)
            .inflate(R.layout.popup_roadmap_actions, null, false)

        val popup = PopupWindow(
            v,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        v.findViewById<TextView>(R.id.itemEditRoadmap).setOnClickListener {
            popup.dismiss()
            // 편집 화면으로 이동
            val args = Bundle().apply {
                putLong("feedId", requireArguments().getLong("feedId"))
                putString("writerNickname", requireArguments().getString("writerNickname"))
            }
            findNavController().navigate(R.id.roadmapEdit, args)
        }

        v.findViewById<TextView>(R.id.itemDeleteRoadmap).setOnClickListener {
            popup.dismiss()
            confirmDeleteRoadmap()
        }

        showPopupBelowRight(popup, anchor, v)
    }

    fun showPopupBelowRight(popup: PopupWindow, anchor: View, contentView: View) {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = contentView.measuredWidth
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }


    // ✅ 삭제 확인 (API 미정 → TODO)
    private fun confirmDeleteRoadmap() {
        AlertDialog.Builder(requireContext())
            .setTitle("로드맵을 삭제할까요?")
            .setMessage("되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { d, _ ->
                // TODO: 로드맵 삭제 API 연동
                Toast.makeText(requireContext(), "삭제 기능은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .show()
    }
}
