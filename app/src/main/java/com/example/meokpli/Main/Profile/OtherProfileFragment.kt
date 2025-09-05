package com.example.meokpli.Main.Profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.OtherUserPageResponse
import com.example.meokpli.Main.SocialInteractionApi
import com.example.meokpli.Main.Interaction.FollowApi
import com.example.meokpli.Main.Interaction.GetFollowResponseDto
import com.example.meokpli.Main.SlicedResponse
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * 남의 프로필 화면 (V2 응답만 사용)
 * - 기간/지역 탭
 * - 연도 헤더 + 2열 그리드, 지역별 가로 썸네일
 * - 팔로우 토글
 * - 팔로워/팔로잉 이동
 * - '관계 API'가 없으므로, 내 팔로잉/팔로워 일부 페이지를 훑어 관계를 추정
 */
class OtherProfileFragment : Fragment() {

    private val socialApi: SocialInteractionApi by lazy { Network.socialApi(requireContext()) }
    private val followApi: FollowApi by lazy { Network.followApi(requireContext()) }

    private var nickname: String = ""
    private var isMe: Boolean = false
    private var isFollowing: Boolean = false   // 내가 그를 팔로우 중?
    private var followsMe: Boolean = false     // 그가 나를 팔로우 중?
    private var followersCount: Long = 0L

    // 상단 뷰
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

    // 탭/리스트
    private lateinit var rvMyFeeds: RecyclerView
    private lateinit var textTabPeriod: TextView
    private lateinit var textTabRegion: TextView
    private lateinit var indicatorPeriod: View
    private lateinit var indicatorRegion: View
    private lateinit var tabPeriod: LinearLayout
    private lateinit var tabRegion: LinearLayout
    private lateinit var feedsAdapter: MyFeedThumbnailAdapter
    private var isPeriodTab = true

    // 마지막으로 받아온 V2 페이지
    private var lastOtherPage: OtherUserPageResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nickname = requireArguments().getString(ARG_NICKNAME)?.trim().orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        bindClicks()
        setupList()
        if (nickname.isBlank()) {
            Toast.makeText(requireContext(), "잘못된 접근입니다(닉네임 없음).", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        android.util.Log.d("OtherProfile", "open for nickname='${nickname}'")
        loadProfile()

    }

    private fun bindViews(root: View) {
        // 상단
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

        // 남의 프로필: 설정 영역 숨김, 팔로우 버튼 노출
        tvSettings.visibility = View.GONE
        viewSettingsLine.visibility = View.GONE
        btnFollow.visibility = View.VISIBLE

        // 리스트 / 탭
        rvMyFeeds = root.findViewById(R.id.rvMyFeeds)
        rvMyFeeds.isNestedScrollingEnabled = false

        textTabPeriod = root.findViewById(R.id.textTabPeriod)
        textTabRegion = root.findViewById(R.id.textTabRegion)
        indicatorPeriod = root.findViewById(R.id.indicatorPeriod)
        indicatorRegion = root.findViewById(R.id.indicatorRegion)
        tabPeriod = root.findViewById(R.id.tabPeriod)
        tabRegion = root.findViewById(R.id.tabRegion)
    }



    private fun bindClicks() {
        btnBack.setOnClickListener { findNavController().popBackStack() }

        // 타인의 팔로잉/팔로워 리스트로 이동
        tvFollowing.setOnClickListener {
            val args = bundleOf(
                "arg_nickname" to nickname,
                "arg_tab" to "following",
                "arg_followers" to parseIntSafe(tvFollowers.text.toString()),
                "arg_following" to parseIntSafe(tvFollowing.text.toString())
            )
            findNavController().navigate(R.id.otherFollowListFragment, args)
        }
        tvFollowers.setOnClickListener {
            val args = bundleOf(
                "arg_nickname" to nickname,
                "arg_tab" to "followers",
                "arg_followers" to parseIntSafe(tvFollowers.text.toString()),
                "arg_following" to parseIntSafe(tvFollowing.text.toString())
            )
            findNavController().navigate(R.id.otherFollowListFragment, args)
        }

        // 탭 전환
        tabPeriod.setOnClickListener { switchToPeriodTab() }
        tabRegion.setOnClickListener { switchToRegionTab() }

        // 팔로우 토글
        btnFollow.setOnClickListener { toggleFollow() }
    }

    private fun setupList() {
        feedsAdapter = MyFeedThumbnailAdapter()
        rvMyFeeds.adapter = feedsAdapter
    }

    /** V2만 사용 + 관계 추정까지 포함 */
    private fun loadProfile() {
        view ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d("OtherProfile", "call getOtherUserPage nickname='${nickname}'")
                // 1) 타인 프로필 조회
                val wrapper = withContext(Dispatchers.IO) { socialApi.getOtherUserPage(nickname) }
                val res = wrapper.userPageDto

                android.util.Log.d(
                    "OtherProfile",
                    "resp userNickname='${res.userNickname}', isMe=${res.isMe}"
                )
                lastOtherPage = res

// 2) 바인딩
                tvNickname.text   = res.userNickname
                tvIntro.text      = res.userIntro
                tvPost.text       = res.feedNum.toString()
                tvFollowing.text  = res.followingNum.toString()
                tvFollowers.text  = res.followerNum.toString()
                followersCount    = res.followerNum

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

                // 3) 기본 탭 = 기간
                switchToPeriodTab()

                // 4) 내 계정 여부
                isMe = res.isMe
                if (isMe) {
                    tvTitle.text = "내 계정"
                    tvSettings.visibility = View.VISIBLE
                    viewSettingsLine.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    return@launch
                }

                // 5) 타인 계정 UI
                tvTitle.text = "${res.userNickname ?: nickname}의 계정"
                tvSettings.visibility = View.GONE
                viewSettingsLine.visibility = View.GONE
                btnFollow.visibility = View.VISIBLE

                // 6) 관계 API가 없으므로, 내 목록 일부 페이지를 훑어 상태 추정
                val (iFollowHim, heFollowsMe) = resolveRelationshipSlow(
                    targetNickname = nickname,
                    maxPages = 2,
                    pageSize = 10
                )
                isFollowing = iFollowHim
                followsMe = heFollowsMe
                renderFollowUi()

            } catch (e: HttpException) {
                val msg = if (e.code() == 401) "로그인이 필요합니다." else "프로필 로딩 실패 (${e.code()})"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "프로필을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ====== 탭 전환 ======
    private fun switchToPeriodTab() {
        isPeriodTab = true
        val glm = GridLayoutManager(requireContext(), 2)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // 헤더는 2칸, 사진은 1칸
                // (어댑터의 헤더 타입 상수에 의존하지 않기 위해 viewType == 0 가정: TYPE_YEAR_HEADER=0)
                return if (feedsAdapter.getItemViewType(position) == 0) 2 else 1
            }
        }
        rvMyFeeds.layoutManager = glm
        setTabSelected(true)

        lastOtherPage?.let { feedsAdapter.updateItems(buildYearItemsV2(it)) }
    }

    private fun switchToRegionTab() {
        isPeriodTab = false
        rvMyFeeds.layoutManager = LinearLayoutManager(requireContext())
        setTabSelected(false)

        val v2 = lastOtherPage ?: run {
            feedsAdapter.updateItems(emptyList())
            return
        }
        feedsAdapter.updateItems(buildRegionItemsV2(v2))
    }

    private fun setTabSelected(isPeriod: Boolean) {
        val active = Color.BLACK
        val inactive = Color.parseColor("#E8E8E8")
        textTabPeriod.setTextColor(if (isPeriod) active else inactive)
        textTabRegion.setTextColor(if (!isPeriod) active else inactive)
        indicatorPeriod.visibility = if (isPeriod) View.VISIBLE else View.INVISIBLE
        indicatorRegion.visibility = if (!isPeriod) View.VISIBLE else View.INVISIBLE
    }

    // ====== 아이템 빌더(V2) ======
    private fun buildYearItemsV2(p: OtherUserPageResponse): List<MyPageItem> {
        val out = mutableListOf<MyPageItem>()
        p.feedIdsGroupedByYear
            .toSortedMap(compareByDescending { it })
            .forEach { (year, feedIds) ->
                out += MyPageItem.YearHeader(year)
                feedIds.forEach { fid ->
                    p.urlMappedByFeedId[fid]?.let { url -> out += MyPageItem.Photo(url) }
                }
            }
        return out
    }

    private fun buildRegionItemsV2(p: OtherUserPageResponse): List<MyPageItem> {
        return p.feedIdsGroupedByRegion.mapNotNull { (regionKey, feedIds) ->
            val urls = feedIds.mapNotNull { p.urlMappedByFeedId[it] }
            if (urls.isEmpty()) null else MyPageItem.RegionRow(regionKey, urls)
        }
    }

    // ===== 팔로우 토글 =====
    private fun toggleFollow() {
        if (isMe) return
        btnFollow.isEnabled = false

        val target = tvNickname.text?.toString().orEmpty()
        val now = isFollowing

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (now) socialApi.unFollow(target) else socialApi.follow(target)
                }
                isFollowing = !now
                followersCount += if (isFollowing) 1 else -1
                if (followersCount < 0) followersCount = 0
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
            } catch (e: Throwable) {
                Toast.makeText(requireContext(), "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                btnFollow.isEnabled = true
            }
        }
    }

    private fun renderFollowUi() {
        if (isMe) {
            btnFollow.visibility = View.GONE
            return
        }
        btnFollow.visibility = View.VISIBLE
        btnFollow.text = if (isFollowing) "팔로잉" else "팔로우"
        // 필요 시 맞팔 표시:
        // if (!isFollowing && followsMe) btnFollow.text = "맞팔하기"
    }

    // ===== 관계 추정 (관계 API 없음 → 내 팔로잉/팔로워 일부 페이지만 훑음) =====
    private suspend fun resolveRelationshipSlow(
        targetNickname: String,
        maxPages: Int = 2,
        pageSize: Int = 10,
        sort: String = "id,DESC"
    ): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        var iFollowHim = false   // 내가 그를 팔로우 중?
        var heFollowsMe = false  // 그가 나를 팔로우 중?

        // 1) 내 팔로잉 목록에서 target 찾기 → 내가 그를 팔로우 중?
        run {
            var page = 0
            var hasNext = true
            while (page < maxPages && hasNext && !iFollowHim) {
                val resp: SlicedResponse<GetFollowResponseDto> =
                    followApi.getFollowingList(page = page, size = pageSize, sort = sort)
                iFollowHim = resp.content.any { it.nickname == targetNickname }
                hasNext = resp.hasNext
                page++
            }
        }

        // 2) 내 팔로워 목록에서 target 찾기 → 그가 나를 팔로우 중?
        run {
            var page = 0
            var hasNext = true
            while (page < maxPages && hasNext && !heFollowsMe) {
                val resp: SlicedResponse<GetFollowResponseDto> =
                    followApi.getFollowerList(page = page, size = pageSize, sort = sort)
                heFollowsMe = resp.content.any { it.nickname == targetNickname }
                hasNext = resp.hasNext
                page++
            }
        }

        iFollowHim to heFollowsMe
    }

    private fun parseIntSafe(s: String): Int =
        s.filter { it.isDigit() }.toIntOrNull() ?: 0

    companion object {
        private const val ARG_NICKNAME = "arg_nickname"
        fun newInstance(nickname: String) = OtherProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_NICKNAME, nickname) }
        }
    }
}
