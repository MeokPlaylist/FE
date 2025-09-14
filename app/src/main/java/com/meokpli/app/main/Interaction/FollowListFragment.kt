package com.meokpli.app.main.Interaction

import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meokpli.app.auth.Network
import com.meokpli.app.main.SocialInteractionApi
import com.meokpli.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.fragment.findNavController
import com.meokpli.app.main.SlicedResponse


class FollowListFragment : Fragment() {

    enum class FollowTab { FOLLOWERS, FOLLOWING }

    companion object {
        const val ARG_TAB = "arg_tab"         // 선택: 초기 탭
        private const val KEY_TAB = "key_tab" // 마지막 탭 복원
    }

    // Views
    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowing: TextView
    private lateinit var indicatorFollowers: View
    private lateinit var indicatorFollowing: View
    private lateinit var headerTitle: TextView
    private lateinit var headerCount: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    // Adapter
    private lateinit var adapter: FollowUserAdapter

    // State
    private var currentTab = FollowTab.FOLLOWING
    private var currentPage = 0
    private var isLoading = false
    private var isLastPage = false
    private var currentTotalCount: Long = 0L

    // 이미지 키 → 절대 URL
    private val IMAGE_BASE = "https://meokplaylist.store/images/"

    // APIs
    private lateinit var api: FollowApi                 // /user/ 목록
    private lateinit var socialApi: SocialInteractionApi // /socialInteraction/ 액션

    // ✅ 내 관계 캐시
    private val myFollowingSet = mutableSetOf<String>() // 내가 팔로우 중
    private val myFollowersSet = mutableSetOf<String>() // 나를 팔로우 중

    override fun onAttach(context: Context) {
        super.onAttach(context)
        api = Network.followApi(context)
        socialApi = Network.socialApi(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_follow_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 마지막 탭 복원 > 인자 초기 탭 > 기본 FOLLOWING
        currentTab = savedInstanceState?.getString(KEY_TAB)?.let { FollowTab.valueOf(it) }
            ?: arguments?.getString(ARG_TAB)?.let { FollowTab.valueOf(it) }
                    ?: FollowTab.FOLLOWING

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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
            onItemClick = { user ->
                findNavController().navigate(
                    R.id.otherProfileFragment,
                    bundleOf("arg_nickname" to user.name)
                )
            },
            onActionClick = { user ->
                // ✅ 탭에 따라 액션이 달라짐
                if (currentTab == FollowTab.FOLLOWING) {
                    // FOLLOWING 탭: 나는 이 사람을 팔로우 중 → 언팔하면 리스트에서 제거
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { socialApi.unFollow(nickname = user.name) }
                            myFollowingSet.remove(user.name) // Set 동기화
                            val newList = adapter.currentList.filter { it.id != user.id }
                            adapter.submitList(newList)
                            if (currentTotalCount > 0) currentTotalCount -= 1
                            headerCount.text = "%,d".format(currentTotalCount)
                            if (newList.isEmpty()) {
                                showEmpty("팔로잉이 없습니다.")
                            }
                            Toast.makeText(requireContext(), "언팔로우 완료", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(requireContext(), "언팔로우 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // FOLLOWERS 탭: 그가 나를 팔로우 중 → 내 상태에 따라 팔로우/언팔 토글, 행은 유지
                    lifecycleScope.launch {
                        try {
                            val isFollowingNow = myFollowingSet.contains(user.name)
                            if (isFollowingNow) {
                                withContext(Dispatchers.IO) { socialApi.unFollow(nickname = user.name) }
                                myFollowingSet.remove(user.name)
                                // 상태만 false로
                                val newList = adapter.currentList.map {
                                    if (it.id == user.id) it.copy(isFollowing = false) else it
                                }
                                adapter.submitList(newList)
                                Toast.makeText(requireContext(), "언팔로우 완료", Toast.LENGTH_SHORT).show()
                            } else {
                                withContext(Dispatchers.IO) { socialApi.follow(nickname = user.name) }
                                myFollowingSet.add(user.name)
                                val newList = adapter.currentList.map {
                                    if (it.id == user.id) it.copy(isFollowing = true) else it
                                }
                                adapter.submitList(newList)
                                Toast.makeText(requireContext(), "팔로우 완료", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(requireContext(), "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
        recycler.adapter = adapter

        // 무한 스크롤(끝에서 3개 남았을 때 다음 페이지 로드)
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (!isLoading && !isLastPage && lastVisible >= total - 3) {
                    if (currentTab == FollowTab.FOLLOWERS) {
                        loadFollowerPage(currentPage + 1, append = true)
                    } else {
                        loadFollowingPage(currentPage + 1, append = true)
                    }
                }
            }
        })

        // 탭 클릭
        view.findViewById<View>(R.id.boxFollowers).setOnClickListener { switchTab(FollowTab.FOLLOWERS) }
        view.findViewById<View>(R.id.boxFollowing).setOnClickListener { switchTab(FollowTab.FOLLOWING) }

        // ✅ 먼저 두 Set 1페이지 프리로드 → 이후 탭 로드 (초기 표시 정확도 ↑)
        lifecycleScope.launch {
            try {
                val following0 = withContext(Dispatchers.IO) { api.getFollowingList(page = 0) }
                myFollowingSet += following0.content.map { it.nickname }
                val followers0 = withContext(Dispatchers.IO) { api.getFollowerList(page = 0) }
                myFollowersSet += followers0.content.map { it.nickname }
            } catch (_: Exception) { /* 무시해도 동작 */ }
            switchTab(currentTab)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAB, currentTab.name)
    }

    // 탭 전환
    private fun switchTab(tab: FollowTab) {
        currentTab = tab
        currentPage = 0
        isLoading = false
        isLastPage = false
        currentTotalCount = 0
        headerCount.text = "—"

        val active = Color.BLACK
        val inactive = Color.parseColor("#E0E0E0")
        tabFollowers.setTextColor(if (tab == FollowTab.FOLLOWERS) active else inactive)
        tabFollowing.setTextColor(if (tab == FollowTab.FOLLOWING) active else inactive)
        indicatorFollowers.visibility = if (tab == FollowTab.FOLLOWERS) View.VISIBLE else View.INVISIBLE
        indicatorFollowing.visibility = if (tab == FollowTab.FOLLOWING) View.VISIBLE else View.INVISIBLE

        adapter.submitList(emptyList())
        if (tab == FollowTab.FOLLOWERS) {
            headerTitle.text = "My\nFollowers"
            loadFollowerPage(page = 0, append = false)
        } else {
            headerTitle.text = "My\nFollowing"
            loadFollowingPage(page = 0, append = false)
        }
    }

    // 서버 연동
    private fun loadFollowerPage(page: Int, append: Boolean) {
        lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) { api.getFollowerList(page, size = 20) }
                myFollowersSet += resp.content.map { it.nickname }
                if (!append) showLoading(false)
                applySlice(resp, append, "팔로워가 없습니다.")
            } catch (_: Exception) {
                if (!append) showLoading(false)
                if (!append) showEmpty("팔로워 정보를 불러올 수 없습니다.")
            } finally {
                isLoading = false
            }
        }
    }


    private fun loadFollowingPage(page: Int, append: Boolean) {
        lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) { api.getFollowingList(page, size = 20) }
                myFollowersSet += resp.content.map { it.nickname }
                if (!append) showLoading(false)
                applySlice(resp, append, "팔로잉이 없습니다.")
            } catch (_: Exception) {
                if (!append) showLoading(false)
                if (!append) showEmpty("팔로잉 정보를 불러올 수 없습니다.")
            } finally {
                isLoading = false
            }
        }
    }


    // Page 응답 공통 반영 (상태 보정 포함)
    // SliceResponse 사용
    private fun applySlice(
        slice: SlicedResponse<GetFollowResponseDto>,
        append: Boolean,
        emptyMsg: String
    ) {
        val newUi = slice.content.map { dto ->
            val base = FollowUserUi(
                id = dto.nickname.hashCode().toLong(),
                name = dto.nickname,
                subtitle = dto.introduction.orEmpty(),
                avatarUrl = dto.profileImgKey?.let { IMAGE_BASE + it }
            )
            when (currentTab) {
                FollowTab.FOLLOWING -> base.copy(
                    isFollowing = true,
                    followsMe = myFollowersSet.contains(dto.nickname)
                )
                FollowTab.FOLLOWERS -> base.copy(
                    isFollowing = myFollowingSet.contains(dto.nickname),
                    followsMe = true
                )
            }
        }

        val merged = if (append) adapter.currentList + newUi else newUi
        if (merged.isEmpty()) {
            showEmpty(emptyMsg)
        } else {
            hideEmpty()
            adapter.submitList(merged)
        }

        currentPage = slice.page
        isLastPage = !slice.hasNext   // ✅ Slice는 last 대신 hasNext
        currentTotalCount = merged.size.toLong() // ✅ totalElements 대신 지금까지 merge한 size
        headerCount.text = "%,d".format(currentTotalCount)
    }

    // 로딩/빈뷰
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    private fun showEmpty(msg: String) {
        emptyView.text = msg
        emptyView.visibility = View.VISIBLE
        recycler.visibility = View.GONE
    }
    private fun hideEmpty() {
        emptyView.visibility = View.GONE
        recycler.visibility = View.VISIBLE
    }
}
