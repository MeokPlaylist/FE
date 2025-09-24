package com.meokpli.app.main.Interaction

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.main.SlicedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG_FL = "FollowList"

class FollowListFragment : Fragment() {

    enum class FollowTab { FOLLOWERS, FOLLOWING }

    companion object { const val ARG_TAB = "arg_tab" }

    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowing: TextView
    private lateinit var indicatorFollowers: View
    private lateinit var indicatorFollowing: View
    private lateinit var headerTitle: TextView
    private lateinit var headerCount: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private lateinit var adapter: FollowUserAdapter

    private var currentTab = FollowTab.FOLLOWING
    private var currentPage = 0
    private var isLoading = false
    private var isLastPage = false
    private var currentTotalCount: Int = 0

    private lateinit var followApi: FollowApi

    // 관계 캐시 (1페이지 프리로드)
    private val myFollowingSet = mutableSetOf<String>() // 내가 팔로우 중
    private val myFollowersSet = mutableSetOf<String>() // 나를 팔로우함

    private var myNickname: String? = null

    // 헤더 “총합” (프로필에서 전달)
    private var initialFollowers: Int = 0
    private var initialFollowing: Int = 0
    private var headerTotal: Int = 0 // 현재 탭의 총합 표시값

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        followApi = Network.followApi(requireContext())
        currentTab = when (requireArguments().getString(ARG_TAB)) {
            "FOLLOWERS" -> FollowTab.FOLLOWERS
            else -> FollowTab.FOLLOWING
        }
        // 프로필에서 넘어온 총합
        initialFollowers = requireArguments().getInt("arg_followers", 0)
        initialFollowing = requireArguments().getInt("arg_following", 0)
        Log.d(TAG_FL, "args totals followers=$initialFollowers following=$initialFollowing")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_follow_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabFollowers = view.findViewById(R.id.tabFollowers)
        tabFollowing = view.findViewById(R.id.tabFollowing)
        indicatorFollowers = view.findViewById(R.id.indicatorFollowers)
        indicatorFollowing = view.findViewById(R.id.indicatorFollowing)
        headerTitle = view.findViewById(R.id.headerTitle)
        headerCount = view.findViewById(R.id.headerCount)
        recycler = view.findViewById(R.id.recycler)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.emptyView)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = FollowUserAdapter(
            onItemClick = { row ->
                Log.d(TAG_FL, "navigate to profile of ${row.name}")
                findNavController().navigate(
                    R.id.otherProfileFragment,
                    bundleOf("arg_nickname" to row.name)
                )
            },
            onActionClick = { row ->
                // 요구사항:
                // - FOLLOWING 탭: 언팔 성공 시 리스트에서 제거
                // - FOLLOWERS 탭: 팔로우/언팔 토글만 하고 행은 유지
                when (currentTab) {
                    FollowTab.FOLLOWING -> toggleFollowAndRemove(row)
                    FollowTab.FOLLOWERS -> toggleFollowOnly(row)
                }
            }
        )
        recycler.adapter = adapter

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        tabFollowers.setOnClickListener { switchTab(FollowTab.FOLLOWERS) }
        tabFollowing.setOnClickListener { switchTab(FollowTab.FOLLOWING) }

        // 초기 데이터 로드
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) 내 닉네임 불러와서 어댑터에 주입 (내 항목 버튼 숨김)
            runCatching { withContext(Dispatchers.IO) { Network.userApi(requireContext()).getPersonalInfo() } }
                .onSuccess {
                    myNickname = it.name
                    adapter.setMyNickname(myNickname)
                    Log.d(TAG_FL, "myNickname=$myNickname")
                }
                .onFailure {
                    adapter.setMyNickname(null)
                    Log.w(TAG_FL, "failed to load personal info", it)
                }

            // 2) 관계 캐시 프리로드(각 1페이지) 관계api있음 교체
            runCatching { withContext(Dispatchers.IO) { followApi.getFollowingList(page = 0) } }
                .onSuccess { myFollowingSet += it.content.map { dto -> dto.nickname }; Log.d(TAG_FL, "prefetch following=${myFollowingSet.size}") }
                .onFailure { Log.w(TAG_FL, "prefetch following fail", it) }

            runCatching { withContext(Dispatchers.IO) { followApi.getFollowerList(page = 0) } }
                .onSuccess { myFollowersSet += it.content.map { dto -> dto.nickname }; Log.d(TAG_FL, "prefetch followers=${myFollowersSet.size}") }
                .onFailure { Log.w(TAG_FL, "prefetch followers fail", it) }

            switchTab(currentTab)
        }

        // 무한 스크롤
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (!isLoading && !isLastPage && last >= total - 3) {
                    when (currentTab) {
                        FollowTab.FOLLOWERS -> loadFollowerPage(currentPage + 1, true)
                        FollowTab.FOLLOWING -> loadFollowingPage(currentPage + 1, true)
                    }
                }
            }
        })
    }

    private fun switchTab(tab: FollowTab) {
        currentTab = tab
        isLoading = false
        isLastPage = false
        currentPage = 0
        currentTotalCount = 0
        adapter.submitList(emptyList())

        headerTotal = when (tab) {
            FollowTab.FOLLOWERS -> initialFollowers
            FollowTab.FOLLOWING -> initialFollowing
        }

        when (tab) {
            FollowTab.FOLLOWERS -> {
                headerTitle.text = "Followers"
                setTabSelected(followers = true)
                loadFollowerPage(0, false)
            }
            FollowTab.FOLLOWING -> {
                headerTitle.text = "Following"
                setTabSelected(followers = false)
                loadFollowingPage(0, false)
            }
        }
    }

    private fun setTabSelected(followers: Boolean) {
        val active = Color.BLACK
        val inactive = Color.parseColor("#E8E8E8")
        tabFollowers.setTextColor(if (followers) active else inactive)
        tabFollowing.setTextColor(if (!followers) active else inactive)
        indicatorFollowers.visibility = if (followers) View.VISIBLE else View.INVISIBLE
        indicatorFollowing.visibility = if (!followers) View.VISIBLE else View.INVISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        emptyView.visibility = View.GONE
    }

    private fun showEmpty(msg: String) {
        emptyView.text = msg
        emptyView.visibility = View.VISIBLE
    }

    private fun applySlice(
        resp: SlicedResponse<GetFollowResponseDto>,
        append: Boolean,
        emptyMsg: String
    ) {
        isLastPage = !resp.hasNext
        currentPage = resp.page

        val mapped = resp.content.map { d ->
            FollowUserUi(
                id = (d.nickname.hashCode().toLong() and 0x7FFFFFFF),
                name = d.nickname,
                subtitle = d.introduction.orEmpty(),
                avatarUrl = d.profileImgKey,
                isFollowing = when (currentTab) {
                    FollowTab.FOLLOWING -> true
                    FollowTab.FOLLOWERS -> myFollowingSet.contains(d.nickname)
                },
                followsMe = when (currentTab) {
                    FollowTab.FOLLOWERS -> true
                    FollowTab.FOLLOWING -> myFollowersSet.contains(d.nickname)
                }
            )
        }

        Log.d(TAG_FL, "apply page=${resp.page} size=${mapped.size} next=${resp.hasNext}")

        if (append) {
            val cur = adapter.currentList.toMutableList()
            cur += mapped
            adapter.submitList(cur)
        } else {
            if (mapped.isEmpty()) showEmpty(emptyMsg)
            else {
                emptyView.visibility = View.GONE
                adapter.submitList(mapped)
            }
        }

        headerCount.text = "%,d".format(headerTotal)
    }

    private fun loadFollowerPage(page: Int, append: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) { followApi.getFollowerList(page = page, size = 20) }
                if (!append) showLoading(false)
                applySlice(resp, append, emptyMsg = "팔로워가 없습니다.")
            } catch (e: Exception) {
                Log.e(TAG_FL, "loadFollowerPage error p=$page", e)
                if (!append) {
                    showLoading(false)
                    showEmpty("팔로워 정보를 불러올 수 없습니다.")
                }
            } finally { isLoading = false }
        }
    }

    private fun loadFollowingPage(page: Int, append: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) { followApi.getFollowingList(page = page, size = 20) }
                if (!append) showLoading(false)
                applySlice(resp, append, emptyMsg = "팔로잉이 없습니다.")
            } catch (e: Exception) {
                Log.e(TAG_FL, "loadFollowingPage error p=$page", e)
                if (!append) {
                    showLoading(false)
                    showEmpty("팔로잉 정보를 불러올 수 없습니다.")
                }
            } finally { isLoading = false }
        }
    }

    /** FOLLOWING 탭: 언팔 성공 시 행 제거 */
    private fun toggleFollowAndRemove(row: FollowUserUi) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!row.isFollowing) {
                    // 안전 가드: 비정상 상태면 팔로우로 살려두기
                    Network.socialApi(requireContext()).follow(row.name)
                    myFollowingSet.add(row.name)
                    row.isFollowing = true
                    updateRow(row)
                    Toast.makeText(requireContext(), "팔로우했습니다.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG_FL, "follow done name=${row.name}")
                } else {
                    Network.socialApi(requireContext()).unFollow(row.name)
                    myFollowingSet.remove(row.name)
                    // FOLLOWING 탭은 리스트에서 제거
                    val cur = adapter.currentList.toMutableList()
                    val idx = cur.indexOfFirst { it.id == row.id }
                    if (idx >= 0) {
                        cur.removeAt(idx)
                        adapter.submitList(cur)

                    }
                    // 헤더 총합 1 감소
                    headerTotal = (headerTotal - 1).coerceAtLeast(0)
                    headerCount.text = "%,d".format(headerTotal)
                    // 프로필로 델타 전달
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("followingDelta", -1)

                    Log.d(TAG_FL, "unfollow done name=${row.name} (removed) total=$headerTotal")
                }
            } catch (t: Throwable) {
                Log.e(TAG_FL, "toggleFollowAndRemove failed name=${row.name}", t)
            }
        }
    }

    /** FOLLOWERS 탭: 토글만 하고 행은 유지 */
    private fun toggleFollowOnly(row: FollowUserUi) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (row.isFollowing) {
                    Network.socialApi(requireContext()).unFollow(row.name)
                    myFollowingSet.remove(row.name)
                    row.isFollowing = false
                    updateRow(row)
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("followingDelta", -1)
                    Log.d(TAG_FL, "unfollow done name=${row.name}")
                } else {
                    Network.socialApi(requireContext()).follow(row.name)
                    myFollowingSet.add(row.name)
                    row.isFollowing = true
                    updateRow(row)
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("followingDelta", +1)
                    Log.d(TAG_FL, "follow done name=${row.name}")
                }
            } catch (t: Throwable) {
                Log.e(TAG_FL, "toggleFollowOnly failed name=${row.name}", t)
            }
        }
    }

    private fun updateRow(row: FollowUserUi) {
        val cur = adapter.currentList.toMutableList()
        val idx = cur.indexOfFirst { it.id == row.id }
        if (idx >= 0) cur[idx] = row.copy()
        adapter.submitList(cur)
    }
}
