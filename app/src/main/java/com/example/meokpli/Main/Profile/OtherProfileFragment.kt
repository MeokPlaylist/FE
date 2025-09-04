package com.example.meokpli.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.Interaction.FollowApi
import com.example.meokpli.Main.Interaction.OtherFollowListFragment
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OtherProfileFragment : Fragment() {

    private val socialApi: SocialInteractionApi by lazy { Network.socialApi(requireContext()) }
    private val followApi: FollowApi by lazy { Network.followApi(requireContext()) }

    private var nickname: String = ""
    private var isMe = false
    private var isFollowing = false   // 나 → 그
    private var followsMe = false     // 그 → 나
    private var followersCount = 0L

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvIntro: TextView
    private lateinit var tvPost: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var tvFollowers: TextView
    private lateinit var tvSettings: TextView
    private lateinit var viewSettingsLine: View
    private lateinit var btnFollow: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nickname = requireArguments().getString(ARG_NICKNAME).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        bindClicks()
        loadProfile()
    }

    private fun bindViews(root: View) {
        btnBack = root.findViewById(R.id.btnBack)
        ivAvatar = root.findViewById(R.id.imageAvatar)
        tvTitle = root.findViewById(R.id.textTitle)
        tvNickname = root.findViewById(R.id.textNickname)
        tvIntro = root.findViewById(R.id.textBio)
        tvPost = root.findViewById(R.id.textPostCount)
        tvFollowing = root.findViewById(R.id.textFollowing)
        tvFollowers = root.findViewById(R.id.textFollowers)
        tvSettings = root.findViewById(R.id.textSettings)
        viewSettingsLine = root.findViewById(R.id.viewSettingsLine)
        btnFollow = root.findViewById(R.id.btnFollow)
    }

    private fun bindClicks() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // 필요 시 '타인의 팔로워/팔로잉 리스트' 화면으로 이동
        tvFollowing.setOnClickListener {
            findNavController().navigate(
                R.id.otherFollowListFragment,
                bundleOf(
                    "arg_nickname" to nickname,
                    "arg_tab" to "following"
                )
            )
        }
        tvFollowers.setOnClickListener {
            findNavController().navigate(
                R.id.otherFollowListFragment,
                bundleOf(
                    "arg_nickname" to nickname,
                    "arg_tab" to "followers"
                )
            )
        }


        btnFollow.setOnClickListener { toggleFollow() }
    }

    private fun loadProfile() {
        view ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1) 타인 프로필 조회
                val res = withContext(Dispatchers.IO) { socialApi.getUserPage(nickname) }

                // 2) 바인딩 (널 안전)
                tvNickname.text = res.userNickname ?: nickname
                tvIntro.text = res.userIntro.orEmpty()
                tvPost.text = (res.feedNum ?: 0L).toString()
                tvFollowing.text = (res.followingNum ?: 0L).toString()
                tvFollowers.text = (res.followerNum ?: 0L).toString()
                followersCount = res.followerNum ?: 0L

                val avatarUrl = res.profileUrl
                if (!avatarUrl.isNullOrBlank()) {
                    ivAvatar.load(avatarUrl) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile_red)
                }

                // 3) 내 계정 여부
                isMe = res.isMe == true
                if (isMe) {
                    tvTitle.text = "내 계정"
                    tvSettings.visibility = View.VISIBLE
                    viewSettingsLine.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    return@launch
                }

                // 4) 타인 계정 UI
                tvTitle.text = "${res.userNickname ?: nickname}의 계정"
                tvSettings.visibility = View.GONE
                viewSettingsLine.visibility = View.GONE
                btnFollow.visibility = View.VISIBLE

                // 5) 관계 C안: 내 목록 일부 페이지를 훑어 상태 추정
                val (f1, f2) = resolveRelationshipSlow(nickname, maxPages = 2, pageSize = 10)
                isFollowing = f1
                followsMe = f2
                renderFollowUi()

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                android.util.Log.e("OtherProfile", "HTTP ${e.code()} body=$body", e)
                val msg = if (e.code() == 401) "로그인이 필요합니다." else "프로필 로딩 실패 (${e.code()})"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("OtherProfile", "Fail getUserPage", e)
                Toast.makeText(requireContext(), "프로필을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun resolveRelationshipSlow(
        otherNickname: String,
        maxPages: Int = 2,
        pageSize: Int = 10
    ): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        val followingJob = async {
            var page = 0
            while (page < maxPages) {
                val resp = followApi.getFollowingList(page = page, size = pageSize)
                if (resp.content.any { it.nickname == otherNickname }) return@async true
                if (resp.last || resp.content.isEmpty()) break
                page++
            }
            false
        }
        val followersJob = async {
            var page = 0
            while (page < maxPages) {
                val resp = followApi.getFollowerList(page = page, size = pageSize)
                if (resp.content.any { it.nickname == otherNickname }) return@async true
                if (resp.last || resp.content.isEmpty()) break
                page++
            }
            false
        }
        followingJob.await() to followersJob.await()
    }

    private fun renderFollowUi() {
        when {
            isFollowing && followsMe -> {
                btnFollow.text = "팔로잉"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            isFollowing -> {
                btnFollow.text = "팔로잉"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            followsMe -> {
                btnFollow.text = "맞팔로우"
                btnFollow.setBackgroundResource(R.drawable.btn_mutual_follow)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
            else -> {
                btnFollow.text = "팔로우"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
        btnFollow.isEnabled = true
    }

    private fun toggleFollow() {
        if (isMe) return
        btnFollow.isEnabled = false

        val prevFollowing = isFollowing
        val prevFollowersCount = followersCount

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isFollowing) {
                    withContext(Dispatchers.IO) { socialApi.unFollow(nickname) }
                    isFollowing = false
                    followersCount = (followersCount - 1L).coerceAtLeast(0L)
                } else {
                    withContext(Dispatchers.IO) { socialApi.follow(nickname) }
                    isFollowing = true
                    followersCount = followersCount + 1L
                }
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
            } catch (e: Exception) {
                isFollowing = prevFollowing
                followersCount = prevFollowersCount
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
                Toast.makeText(requireContext(), "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                btnFollow.isEnabled = true
            }
        }
    }

    companion object {
        private const val ARG_NICKNAME = "arg_nickname"
        fun newInstance(nickname: String) = OtherProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_NICKNAME, nickname) }
        }
    }
}
