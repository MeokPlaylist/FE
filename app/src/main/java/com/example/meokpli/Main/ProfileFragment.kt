package com.example.meokpli.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.meokpli.R
import com.example.meokpli.Auth.Network
//import com.example.meokpli.User.MeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private lateinit var avatar: ImageView
    private lateinit var nick: TextView
    private lateinit var bio: TextView
    private lateinit var postCount: TextView
    private lateinit var following: TextView
    private lateinit var followers: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false) // 너가 만든 XML

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------- 뷰 바인딩 --------
        avatar = view.findViewById(R.id.imageAvatar)
        nick = view.findViewById(R.id.textNickname)
        bio = view.findViewById(R.id.textBio)
        postCount = view.findViewById(R.id.textPostCount)
        following = view.findViewById(R.id.textFollowing)
        followers = view.findViewById(R.id.textFollowers)

//        // 뒤로(상단 좌측 화살표): 직전 탭으로
//        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
//            (activity as? MainActivity)?.goToLastTab()
//                ?: requireActivity().onBackPressedDispatcher.onBackPressed()
//        }
//
//        // 시스템 뒤로도 동일 동작 (프로필은 탭 루트이므로 직전 탭으로)
//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    val fm = parentFragmentManager
//                    if (fm.backStackEntryCount > 0) {
//                        fm.popBackStack()
//                    } else {
//                        (activity as? MainActivity)?.goToLastTab() ?: requireActivity().finish()
//                    }
//                }
//            }
//        )
        val textSettings = view.findViewById<TextView>(R.id.textSettings)
        textSettings.setOnClickListener {
            findNavController().navigate(R.id.fragmentSetting)
        }



        //팔로잉/팔로워 화면으로 이동 (상단 탭 있는 리스트)
        following.setOnClickListener {
            val followersCnt = followers.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val followingCnt = following.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val args = bundleOf(
                "arg_tab" to "FOLLOWING",
                "arg_followers" to followersCnt,
                "arg_following" to followingCnt
            )
            findNavController().navigate(R.id.followListFragment, args)
        }

        followers.setOnClickListener {
            val followersCnt = followers.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val followingCnt = following.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val args = bundleOf(
                "arg_tab" to "FOLLOWERS",
                "arg_followers" to followersCnt,
                "arg_following" to followingCnt
            )
            findNavController().navigate(R.id.followListFragment, args)
        }

        // 서버에서 내 프로필 불러오기 (Authorization은 AuthInterceptor가 자동 첨부)
        fetchProfile()
    }
    private fun fetchProfile() {
        lifecycleScope.launch {
            try {
                val api = Network.userApi(requireContext())
                val myPage = api.getMyPage()

                // UI 반영
                nick.text = myPage.userNickname
                bio.text = myPage.userIntro
                postCount.text = myPage.feedNum.toString()
                following.text = myPage.followingNum.toString()
                followers.text = myPage.followerNum.toString()

                // 프로필 이미지 (없으면 기본 아이콘)
                val url = myPage.profileUrl
                if (!url.isNullOrBlank()) {
                    avatar.load(url) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else {
                    avatar.setImageResource(R.drawable.ic_profile_red)
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    // 토큰 만료/비로그인: 로컬 토큰 비우고 로그인 화면으로 전환하는 로직을 여기에.
                    // TokenManager(requireContext()).clear()
                    // startActivity(Intent(requireContext(), LoginActivity::class.java))
                }
                // TODO: 스낵바/토스트로 실패 안내
            } catch (e: Exception) {
                // TODO: 네트워크 오류 등 안내
            }
        }
    }
}
