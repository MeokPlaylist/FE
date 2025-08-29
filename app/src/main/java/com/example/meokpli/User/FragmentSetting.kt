package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.meokpli.R

class FragmentSetting : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 프로필 편집으로 이동
        view.findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // 계정 센터로 이동
        view.findViewById<View>(R.id.rowAccountCenter).setOnClickListener {
            startActivity(Intent(requireContext(), AccountCenterActivity::class.java))
        }
        //이용약관으로 이동
        view.findViewById<View>(R.id.rowPolicy).setOnClickListener {
            startActivity(Intent(requireContext(), TermsActivity::class.java))
        }

        // 뒤로가기 버튼 처리
        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
