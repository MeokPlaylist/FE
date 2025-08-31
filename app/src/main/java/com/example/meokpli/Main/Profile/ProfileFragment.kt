package com.example.meokpli.Main.Profile

import android.graphics.Color
import com.example.meokpli.Main.Profile.MyFeedThumbnailAdapter
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
import com.example.meokpli.R
import com.example.meokpli.Auth.Network
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.data.remote.response.MyPageResponse


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
        backBtn = view.findViewById(R.id.btnBack)
        regionBtn = view.findViewById(R.id.tabRegion)
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
            val followersCnt = followers.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val followingCnt = following.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
            val args = bundleOf(
                "arg_tab" to "FOLLOWING",
                "arg_followers" to followersCnt,
                "arg_following" to followingCnt
            )
            findNavController().navigate(R.id.followListFragment, args)
        }
        backBtn.setOnClickListener { (requireActivity() as? MainActivity)?.handleSystemBack() }
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
// 어댑터 준비
        val adapter = MyFeedThumbnailAdapter(mutableListOf())
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
        lifecycleScope.launch {
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

    // 연도별: 그대로 사용 (헤더 + 사진 아이템 나열)
    private fun buildYearItems(myPage: MyPageResponse): List<MyPageItem> {
        val items = mutableListOf<MyPageItem>()
        myPage.feedIdsGroupedByYear.toSortedMap(compareByDescending { it })
            .forEach { (year, feedIds) ->
                items.add(MyPageItem.YearHeader(year))
                feedIds.forEach { feedId ->
                    myPage.urlMappedByFeedId[feedId]?.let { url ->
                        items.add(MyPageItem.Photo(url))
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
                val photoUrls = feedIds.mapNotNull { myPage.urlMappedByFeedId[it] }
                if (photoUrls.isNotEmpty()) {
                    items.add(MyPageItem.RegionRow(region, photoUrls))
                }
            }
        return items
    }
}


sealed class MyPageItem {
    data class YearHeader(val year: Int) : MyPageItem()
    data class Photo(val url: String) : MyPageItem()   // 연도별 전용
    data class RegionRow(val region: String, val photoUrls: List<String>) : MyPageItem()
}


