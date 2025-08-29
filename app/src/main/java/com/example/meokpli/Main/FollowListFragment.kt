package com.example.meokpli.Main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.example.meokpli.Auth.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FollowListFragment : Fragment() {

    enum class FollowTab { FOLLOWERS, FOLLOWING }

    companion object {
        // 외부에서 초기 탭을 지정하고 싶을 때만 사용(선택)
        const val ARG_TAB = "arg_tab"
        // 마지막에 보던 탭 저장/복원용
        private const val KEY_TAB = "key_tab"

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

    // 이미지 키 → 절대 URL 만들 때
    private val IMAGE_BASE = "https://meokplaylist.store/images/"

    // APIs
    private lateinit var api: FollowApi                 // /user/ 목록
    private lateinit var socialApi: SocialInteractionApi // /socialInteraction/ 액션

    override fun onAttach(context: Context) {
        super.onAttach(context)
        api = Network.followApi(context)       // 토큰 포함
        socialApi = Network.socialApi(context) // 토큰 포함
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_follow_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) 마지막 탭 복원 > 인자 초기 탭 > 기본 FOLLOWING
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
                Toast.makeText(requireContext(), "${user.name} 클릭", Toast.LENGTH_SHORT).show()
                // TODO: 프로필 상세로 이동
            },
            onActionClick = { user ->
                lifecycleScope.launch {
                    try {
                        // 서버는 @AuthenticationPrincipal로 내 userId를 읽음
                        socialApi.unFollow(nickname = user.name) //2XX가 아니면 catch로 떨어짐
                        val newList = adapter.currentList.filter { it.id != user.id }//리스트 제거
                        adapter.submitList(newList)
                        // 낙관적 카운트 감소
                        if (currentTotalCount > 0) currentTotalCount -= 1//서버 안부르고 카운트줄이기 그래야 부드러움
                        headerCount.text = "%,d".format(currentTotalCount)
                        if (newList.isEmpty()) {
                            showEmpty(if (currentTab == FollowTab.FOLLOWERS) "팔로워가 없습니다." else "팔로잉이 없습니다.")
                        }
                        Toast.makeText(requireContext(), "언팔로우 완료", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "언팔로우 실패", Toast.LENGTH_SHORT).show()
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

        // 첫 진입/복원 탭으로 로드
        switchTab(currentTab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAB, currentTab.name) // 마지막 탭 저장
    }

    // 탭 전환
    private fun switchTab(tab: FollowTab) {
        currentTab = tab
        currentPage = 0
        isLoading = false
        isLastPage = false
        currentTotalCount = 0
        headerCount.text = "—" // 응답 오면 갱신

        val active = Color.BLACK
        val inactive = Color.parseColor("#E0E0E0")
        tabFollowers.setTextColor(if (tab == FollowTab.FOLLOWERS) active else inactive)
        tabFollowing.setTextColor(if (tab == FollowTab.FOLLOWING) active else inactive)
        indicatorFollowers.visibility = if (tab == FollowTab.FOLLOWERS) View.VISIBLE else View.INVISIBLE
        indicatorFollowing.visibility = if (tab == FollowTab.FOLLOWING) View.VISIBLE else View.INVISIBLE

        adapter.submitList(emptyList()) // 화면 비우고 새로 로드
        if (tab == FollowTab.FOLLOWERS) {
            headerTitle.text = "My\nFollowers"
            loadFollowerPage(page = 0, append = false) // 스프링 페이지는 0부터
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
                val resp: PageResponse<GetFollowResponseDto> =
                    withContext(Dispatchers.IO) { api.getFollowerList(page = page) }
                if (!append) showLoading(false)
                applyPage(resp, append, emptyMsg = "팔로워가 없습니다.")
            } catch (e: Exception) {
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
                val resp: PageResponse<GetFollowResponseDto> =
                    withContext(Dispatchers.IO) { api.getFollowingList(page = page) }
                if (!append) showLoading(false)
                applyPage(resp, append, emptyMsg = "팔로잉이 없습니다.")
            } catch (e: Exception) {
                if (!append) showLoading(false)
                if (!append) showEmpty("팔로잉 정보를 불러올 수 없습니다.")
            } finally {
                isLoading = false
            }
        }
    }

    // Page 응답 공통 반영
    private fun applyPage(
        page: PageResponse<GetFollowResponseDto>,
        append: Boolean,
        emptyMsg: String
    ) {
        val newUi = page.content.map { dto ->
            FollowUserUi(
                id = dto.nickname.hashCode().toLong(), // ⚠️ 임시 키(가능하면 BE가 userId 제공)
                name = dto.nickname,
                subtitle = dto.introduction.orEmpty(),
                avatarUrl = dto.profileImgKey?.let { IMAGE_BASE + it }
            )
        }

        val merged = if (append) adapter.currentList + newUi else newUi

        if (merged.isEmpty()) {
            showEmpty(emptyMsg)
        } else {
            hideEmpty()
            adapter.submitList(merged)
        }

        // 페이지/마지막/총개수 갱신
        currentPage = page.number
        isLastPage = page.last
        currentTotalCount = page.totalElements ?: merged.size.toLong()
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