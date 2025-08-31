package com.example.meokpli.Main.Interaction

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.OtherProfileActivity
import com.example.meokpli.Main.SocialInteractionApi
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtherFollowListFragment : Fragment() {

    companion object {
        private const val ARG_NICKNAME = "arg_nickname"
        private const val ARG_TAB = "arg_tab" // "followers" or "following"

        fun start(host: Context, nickname: String, tab: String) {
            val fragment = OtherFollowListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NICKNAME, nickname)
                    putString(ARG_TAB, tab)
                }
            }
            // host 는 Activity 컨텍스트여야 함
            val activity = host as androidx.appcompat.app.AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment) // 필요시 전용 container id로 변경
                .addToBackStack(null)
                .commit()
        }
    }

    private lateinit var api: FollowApi
    private lateinit var socialApi: SocialInteractionApi

    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowing: TextView
    private lateinit var indicatorFollowers: View
    private lateinit var indicatorFollowing: View
    private lateinit var headerTitle: TextView
    private lateinit var headerCount: TextView
    private lateinit var btnBack: ImageView

    private lateinit var adapter: OtherFollowUserAdapter

    private var targetNickname: String = ""
    private var currentTab: String = "following" // default
    private var isLoading = false
    private var isLastPage = false
    private var currentPage = 0
    private var currentTotalCount = 0
    // 내 관계 캐시(1페이지 프리로드)
    private val myFollowingSet = mutableSetOf<String>()
    private val myFollowersSet = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = Network.followApi(requireContext())
        socialApi = Network.socialApi(requireContext())
        targetNickname = requireArguments().getString(ARG_NICKNAME) ?: ""
        currentTab = requireArguments().getString(ARG_TAB) ?: "following"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // XML 재사용
        return inflater.inflate(R.layout.fragment_follow_list, container, false) // :contentReference[oaicite:6]{index=6}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
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
        adapter = OtherFollowUserAdapter(
            onProfileClick = { user ->
                OtherProfileActivity.start(requireContext(), user.nickname)
            },
            onFollowToggle = { user, isFollowingNow, position ->
                toggleFollow(user.nickname, isFollowingNow) { success, newState ->
                    if (success) {
                        adapter.updateFollowState(position, newState)
                    }
                }
            }
        )
        recycler.adapter = adapter

        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        tabFollowers.setOnClickListener { switchTab("followers") }
        tabFollowing.setOnClickListener { switchTab("following") }
        // 내 관계 1페이지 프리로드 → UI 상태 보정

        lifecycleScope.launch {
            try {
                val f0 = withContext(Dispatchers.IO) { api.getFollowingList(page = 0) }
                myFollowingSet += f0.content.map { it.nickname }
                val r0 = withContext(Dispatchers.IO) { api.getFollowerList(page = 0) }
                myFollowersSet += r0.content.map { it.nickname }
            } catch (_: Exception) { /* 무시(없어도 동작) */ }
            switchTab(currentTab)
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (!isLoading && !isLastPage && lastVisible >= total - 3) {
                    when (currentTab) {
                        "followers" -> loadFollowerPage(currentPage + 1, append = true)
                        else -> loadFollowingPage(currentPage + 1, append = true)
                    }
                }
            }
        })

    }

    private fun switchTab(tab: String) {
        currentTab = tab
        isLoading = false
        isLastPage = false
        currentPage = 0
        currentTotalCount = 0
        adapter.submitList(emptyList())

        // 타이틀
        val prefix = "${targetNickname}"
        if (tab == "followers") {
            headerTitle.text = "${prefix}\nFollowers"
            tabFollowers.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            tabFollowing.setTextColor(0xFFE0E0E0.toInt())
            indicatorFollowers.visibility = View.VISIBLE
            indicatorFollowing.visibility = View.INVISIBLE
            loadFollowerPage(page = 0, append = false)
        } else {
            headerTitle.text = "${prefix}\nFollowing"
            tabFollowing.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            tabFollowers.setTextColor(0xFFE0E0E0.toInt())
            indicatorFollowing.visibility = View.VISIBLE
            indicatorFollowers.visibility = View.INVISIBLE
            loadFollowingPage(page = 0, append = false)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        emptyView.visibility = View.GONE
    }

    private fun showEmpty(msg: String) {
        emptyView.text = msg
        emptyView.visibility = View.VISIBLE
    }

    private fun applyPage(
        resp: PageResponse<GetFollowResponseDto>,
        append: Boolean,
        emptyMsg: String
    ) {
        isLastPage = resp.last
        currentPage = resp.number

        // 상태 보정: 내 팔로잉/팔로워 셋 기준으로 표시
        fun mapToUi(d: GetFollowResponseDto) = UserRowUi(
            nickname = d.nickname,
            profileImgUrl = d.profileImgKey,
            introduction = d.introduction,
            isFollowing = myFollowingSet.contains(d.nickname),
            followsMe  = myFollowersSet.contains(d.nickname)
        )
        val newList = resp.content.map(::mapToUi)

        if (append) {
            adapter.append(newList)
        } else {
            if (newList.isEmpty()) showEmpty(emptyMsg)
            else { emptyView.visibility = View.GONE; adapter.submitList(newList) }
        }

        // 헤더 카운트(가능하면 서버 totalElements 사용)
        headerCount.text = (resp.totalElements ?: (adapter.itemCount)).toString()
    }

    private fun loadFollowerPage(page: Int, append: Boolean) {
        lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.getFollowerListOf(nickname = targetNickname, page = page) // 신규 API
                }
                if (!append) showLoading(false)
                applyPage(resp, append, emptyMsg = "${targetNickname}의 팔로워가 없습니다.")
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
                val resp = withContext(Dispatchers.IO) {
                    api.getFollowingListOf(nickname = targetNickname, page = page) // 신규 API
                }
                if (!append) showLoading(false)
                applyPage(resp, append, emptyMsg = "${targetNickname}의 팔로잉이 없습니다.")
            } catch (e: Exception) {
                if (!append) showLoading(false)
                if (!append) showEmpty("팔로잉 정보를 불러올 수 없습니다.")
            } finally {
                isLoading = false
            }
        }
    }

    private fun toggleFollow(
        targetNickname: String,
        isFollowingNow: Boolean,
        onDone: (Boolean, Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                if (isFollowingNow) {
                    withContext(Dispatchers.IO) { socialApi.unFollow(targetNickname) }
                    myFollowingSet.remove(targetNickname)
                    onDone(true, false)
                } else {
                    withContext(Dispatchers.IO) { socialApi.follow(targetNickname) }
                    myFollowingSet.add(targetNickname)
                    onDone(true, true)
                }
            } catch (e: Exception) {
                onDone(false, isFollowingNow)
            }
        }
    }

    // ======== Adapter/Item ========

    data class UserRowUi(
        val nickname: String,
        val profileImgUrl: String?,
        val introduction: String?,
        var isFollowing: Boolean = false,   // 나 → 그
        var followsMe: Boolean = false      // 그 → 나 (필요시 서버에서 내려주면 사용)
    )


    class OtherFollowUserAdapter(
        private val onProfileClick: (UserRowUi) -> Unit,
        private val onFollowToggle: (UserRowUi, Boolean, Int) -> Unit
    ) : RecyclerView.Adapter<OtherFollowUserAdapter.ItemVH>() {

        private val data = mutableListOf<UserRowUi>()

        fun submitList(list: List<UserRowUi>) { data.clear(); data.addAll(list); notifyDataSetChanged() }
        fun append(list: List<UserRowUi>) { val s=data.size; data.addAll(list); notifyItemRangeInserted(s, list.size) }
        fun updateFollowState(position: Int, isFollowing: Boolean) {
            if (position in data.indices) { data[position].isFollowing = isFollowing; notifyItemChanged(position) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_follow, parent, false)
            return ItemVH(v)
        }
        override fun onBindViewHolder(holder: ItemVH, position: Int) = holder.bind(data[position], position)
        override fun getItemCount(): Int = data.size

        inner class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
            private val name: TextView = itemView.findViewById(R.id.tvName)
            private val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
            private val btn: MaterialButton = itemView.findViewById(R.id.btnAction)

            fun bind(u: UserRowUi, pos: Int) {
                val ctx = itemView.context

                // 아바타
                if (!u.profileImgUrl.isNullOrBlank()) {
                    avatar.load(u.profileImgUrl) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else avatar.setImageResource(R.drawable.ic_profile_red)

                name.text = u.nickname
                subtitle.text = u.introduction.orEmpty()

                // 상태별 텍스트/배경/색상 (+아이콘 원하면 함께 지정)
                when {
                    u.isFollowing && u.followsMe -> {
                        btn.text = "맞팔로잉"
                        btn.setBackgroundResource(R.drawable.btn_basic)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                        // btn.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_check) // 선택
                    }
                    u.isFollowing -> {
                        btn.text = "팔로잉"
                        btn.setBackgroundResource(R.drawable.btn_basic)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                        // btn.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_check)
                    }
                    u.followsMe -> {
                        btn.text = "맞팔로우"
                        btn.setBackgroundResource(R.drawable.btn_mutual_follow)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                        // btn.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_add)
                    }
                    else -> {
                        btn.text = "팔로우"
                        btn.setBackgroundResource(R.drawable.btn_basic)
                        btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                        // btn.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_add)
                    }
                }

                itemView.setOnClickListener { onProfileClick(u) }
                btn.setOnClickListener {
                    btn.isEnabled = false
                    onFollowToggle(u, u.isFollowing, pos)
                    btn.isEnabled = true
                }
            }
        }
    }
}
