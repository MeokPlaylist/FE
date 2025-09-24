package com.meokpli.app.main.Profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.data.remote.response.MyPageResponse
import com.meokpli.app.main.Home.FeedDetailActivity
import com.meokpli.app.main.MainActivity


class ProfileFragment : Fragment() {

    private lateinit var avatar: ImageView
    private lateinit var nick: TextView
    private lateinit var bio: TextView
    private lateinit var postCount: TextView
    private lateinit var following: TextView
    private lateinit var followers: TextView
    private lateinit var backBtn: ImageView
    private lateinit var regionBtn: LinearLayout
    private lateinit var timeBtn: LinearLayout
    private lateinit var rvMyFeeds: RecyclerView
    private lateinit var textTabPeriod: TextView
    private lateinit var textTabRegion: TextView
    private lateinit var indicatorPeriod: View
    private lateinit var indicatorRegion: View
    private lateinit var myPage: MyPageResponse

    private var followingCount: Int = 0
    private var followersCount: Int = 0

    private companion object {
        const val TAG_PROFILE = "Profile"
    }

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
        regionBtn = view.findViewById(R.id.tabRegion)
        backBtn = view.findViewById(R.id.btnBack)
        timeBtn = view.findViewById(R.id.tabPeriod)
        rvMyFeeds = view.findViewById(R.id.rvMyFeeds)
        textTabPeriod = view.findViewById(R.id.textTabPeriod)
        textTabRegion = view.findViewById(R.id.textTabRegion)
        indicatorPeriod = view.findViewById(R.id.indicatorPeriod)
        indicatorRegion = view.findViewById(R.id.indicatorRegion)

        // 초기 상태: 기간별 활성화
        setTabSelected(isPeriod = true)

        val textSettings = view.findViewById<TextView>(R.id.textSettings)
        textSettings.setOnClickListener {
            findNavController().navigate(R.id.fragmentSetting)
        }



        //팔로잉/팔로워 화면으로 이동 (상단 탭 있는 리스트)
        following.setOnClickListener {
            val args = bundleOf(
                "arg_tab" to "FOLLOWING",
                "arg_followers" to followersCount,
                "arg_following" to followingCount
            )
            findNavController().navigate(R.id.followListFragment, args)
        }
        followers.setOnClickListener {
            val args = bundleOf(
                "arg_tab" to "FOLLOWERS",
                "arg_followers" to followersCount,
                "arg_following" to followingCount
            )
            findNavController().navigate(R.id.followListFragment, args)
        }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }

        // 서버에서 내 프로필 불러오기 (Authorization은 AuthInterceptor가 자동 첨부)
// 어댑터 준비
        val adapter = MyFeedThumbnailAdapter(mutableListOf()) { feedId ->
            val intent = Intent(requireContext(), FeedDetailActivity::class.java).apply {
                putExtra("feedId", feedId)
            }
            startActivity(intent)
        }
        rvMyFeeds.layoutManager = GridLayoutManager(requireContext(), 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        0 -> 2
                        else -> 1
                    }
                }
            }
        }
        rvMyFeeds.adapter = adapter

        // myPage 세팅된 뒤 UI와 RecyclerView 갱신
        fetchProfile {
            adapter.updateItems(buildYearItems(myPage))   //  myPage 보장됨
        }

        // 기간별 보기 (2열 그리드)
        timeBtn.setOnClickListener {
            rvMyFeeds.layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItemViewType(position)) {
                            0 -> 2 // 헤더는 전체 폭
                            else -> 1 // 사진은 반폭
                        }
                    }
                }
            }
            adapter.updateItems(buildYearItems(myPage))
            setTabSelected(true)
        }

        regionBtn.setOnClickListener {
            rvMyFeeds.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter.updateItems(buildRegionItems(myPage))
            setTabSelected(false)
        }

        // 팔로우/언팔 리스트에서 돌아올 때 실시간 델타 반영
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("followingDelta")
            ?.observe(viewLifecycleOwner) { delta ->
                // 내 팔로잉 합계만 변동
                followingCount = (followingCount + delta).coerceAtLeast(0)
                following.text = "%,d".format(followingCount)
                Log.d(TAG_PROFILE, "following delta=$delta -> $followingCount")
            }
    }

    private fun setTabSelected(isPeriod: Boolean) {
        if (isPeriod) {
            textTabPeriod.setTextColor(Color.parseColor("#000000"))
            indicatorPeriod.setBackgroundColor(Color.parseColor("#000000"))

            textTabRegion.setTextColor(Color.parseColor("#E8E8E8"))
            indicatorRegion.setBackgroundColor(Color.parseColor("#E8E8E8"))
        } else {
            textTabPeriod.setTextColor(Color.parseColor("#E8E8E8"))
            indicatorPeriod.setBackgroundColor(Color.parseColor("#E8E8E8"))

            textTabRegion.setTextColor(Color.parseColor("#000000"))
            indicatorRegion.setBackgroundColor(Color.parseColor("#000000"))
        }
    }

    private fun fetchProfile(onLoaded: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = Network.userApi(requireContext())
                myPage = api.getMyPage()

                // UI 반영
                nick.text = myPage.userNickname
                bio.text = myPage.userIntro
                postCount.text = myPage.feedNum.toString()
                following.text = myPage.followingNum.toString()
                followers.text = myPage.followerNum.toString()
                Log.d("Profile", "feedIdsGroupedByRegion = ${myPage}")

                postCount.text = "%,d".format(myPage.feedNum)

                // ✅ 서버 응답의 총합으로 상태/표시 동기화
                followingCount = myPage.followingNum.toInt()
                followersCount = myPage.followerNum.toInt()
                following.text = "%,d".format(followingCount)
                followers.text = "%,d".format(followersCount)

                Log.d(TAG_PROFILE, "myPage summary: feedNum=${myPage.feedNum}, " +
                        "years=${myPage.feedIdsGroupedByYear.keys}, " +
                        "regions=${myPage.feedIdsGroupedByRegion.keys}, " +
                        "mapSize=${myPage.urlMappedByFeedId.size}")

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
                //순서보장
                onLoaded()
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    // TODO: 토큰 만료/비로그인 처리
                    Log.w(TAG_PROFILE, "Unauthorized: ${e.code()}")
                } else {
                    Log.e(TAG_PROFILE, "HttpException: ${e.code()}", e)
                }
                // TODO: 스낵바/토스트로 실패 안내
            } catch (e: Exception) {
                Log.e(TAG_PROFILE, "fetchProfile error", e)
            }
        }
    }

    // 연도별: 그대로 사용 (헤더 + 사진 아이템 나열)
    private fun buildYearItems(myPage: MyPageResponse): List<MyPageItem> {
        val items = mutableListOf<MyPageItem>()
        myPage.feedIdsGroupedByYear.toSortedMap(compareByDescending { it })
            .forEach { (year, feedIds) ->
                items.add(MyPageItem.YearHeader(year))
                feedIds.forEach { feedId ->
                    myPage.urlMappedByFeedId[feedId]?.let { url ->
                        items.add(MyPageItem.Photo(feedId, url))
                    }
                }
            }
        return items
    }

    // 지역별: Row 단위로 묶어서 반환
    private fun buildRegionItems(myPage: MyPageResponse): List<MyPageItem> {
        val items = mutableListOf<MyPageItem>()
        myPage.feedIdsGroupedByRegion.toSortedMap()
            .forEach { (region, feedIds) ->
                val photos = feedIds.mapNotNull { fid ->
                    myPage.urlMappedByFeedId[fid]?.let { url ->
                        MyPageItem.Photo(fid, url)   // ✅ feedId + url
                    }
                }
                if (photos.isNotEmpty()) {
                    items.add(MyPageItem.RegionRow(region, photos))
                }
            }
        return items
    }
}

sealed class MyPageItem {
    data class YearHeader(val year: Int) : MyPageItem()
    data class Photo(val feedId: Long, val url: String) : MyPageItem()   // 연도별 전용
    data class RegionRow(val region: String, val photos: List<Photo>) : MyPageItem()
}