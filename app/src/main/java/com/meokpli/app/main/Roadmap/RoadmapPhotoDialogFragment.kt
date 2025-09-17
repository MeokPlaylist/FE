package com.meokpli.app.main.Roadmap

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import coil.load
import com.meokpli.app.databinding.DialogRoadmapPhotoBinding

class RoadmapPhotoDialogFragment : DialogFragment() {

    private var _binding: DialogRoadmapPhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = DialogRoadmapPhotoBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog?.setCanceledOnTouchOutside(true)
        return binding.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val name = requireArguments().getString("title").orEmpty()
        val addr = requireArguments().getString("address").orEmpty()
        val url  = requireArguments().getString("photoUrl")

        binding.tvName.text = name
        binding.tvAddr.text = addr

        // Coil은 http(s)뿐 아니라 file://, content:// 도 지원
        if (!url.isNullOrBlank()) {
            binding.ivLarge.load(url) { crossfade(true) }
        } else {
            // 프리뷰 단계에서 url이 없을 수도 있음 → 적절한 플레이스홀더
            binding.ivLarge.setImageResource(android.R.color.darker_gray)
        }

        // 밖 터치/뒤로가기로 닫힘
        binding.root.setOnClickListener { dismissAllowingStateLoss() }
    }

    override fun onStart() {
        super.onStart()
        // 가로 폭 꽉 차게
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
