package com.meokpli.app.main.Interaction

import android.app.AlertDialog
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.meokpli.app.auth.Network
import com.meokpli.app.main.SlicedResponse
import com.meokpli.app.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG_OFL = "OtherFollowList"

class OtherFollowListFragment : Fragment() {

    private lateinit var api: FollowApi
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
    private var currentTab: String = "following"
    private var isLoading = false
    private var isLastPage = false
    private var currentPage = 0
    private var currentTotalCount = 0

    // 내 관계 캐시 (1페이지 프리로드)
    private val myFollowingSet = mutableSetOf<String>() // 내가 팔로우 중
    private val myFollowersSet = mutableSetOf<String>() // 나를 팔로우함

    // 내 닉네임 (내 항목은 버튼 숨김)
    private var myNickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = Network.followApi(requireContext())
        targetNickname = requireArguments().getString("arg_nickname").orEmpty()
        currentTab = requireArguments().getString("arg_tab") ?: "following"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_follow_list, container, false)

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
            onProfileClick = { row ->
                Log.d(TAG_OFL, "navigate to profile of ${row.nickname}")
                findNavController().navigate(
                    R.id.otherProfileFragment,
                    bundleOf("arg_nickname" to row.nickname)
                )
            },
            onFollowToggle = { user, isFollowingNow, position ->
                Log.d(TAG_OFL, "toggle follow target=${user.nickname} now=$isFollowingNow pos=$position")
                if (isFollowingNow) {
                    // ✅ 언팔로우는 다이얼로그 띄우고 확인 시 실행
                    showUnfollowConfirmDialog(user.nickname, user.profileImgUrl) {
                        toggleFollow(user.nickname, true) { success, newState ->
                            if (success) {
                                adapter.updateFollowState(position, newState)
                                Log.d(TAG_OFL, "unfollow success target=${user.nickname}")
                            } else {
                                Log.e(TAG_OFL, "unfollow failed target=${user.nickname}")
                            }
                        }
                    }
                } else {
                    // ✅ 팔로우는 즉시 실행
                    toggleFollow(user.nickname, false) { success, newState ->
                        if (success) {
                            adapter.updateFollowState(position, newState)
                            Log.d(TAG_OFL, "follow success target=${user.nickname}")
                        } else {
                            Log.e(TAG_OFL, "follow failed target=${user.nickname}")
                        }
                    }
                }
            }
        )
        recycler.adapter = adapter

        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        tabFollowers.setOnClickListener { switchTab("followers") }
        tabFollowing.setOnClickListener { switchTab("following") }

        // 초기 데이터/닉네임/관계 캐시 로드
        viewLifecycleOwner.lifecycleScope.launch {
            // 내 닉네임 로드 → 어댑터에 주입 (내 항목 버튼 숨김)
            runCatching {
                withContext(Dispatchers.IO) { Network.userApi(requireContext()).getPersonalInfo() }
            }.onSuccess {
                myNickname = it.name
                Log.d(TAG_OFL, "myNickname=$myNickname")
                adapter.setMyNickname(myNickname)
            }.onFailure {
                myNickname = null
                adapter.setMyNickname(null)
                Log.w(TAG_OFL, "failed to load personal info", it)
            }

            // 관계 캐시 1페이지 프리로드
            try {
                val f0 = withContext(Dispatchers.IO) { api.getFollowingList(page = 0) }
                myFollowingSet += f0.content.map { it.nickname }
                val r0 = withContext(Dispatchers.IO) { api.getFollowerList(page = 0) }
                myFollowersSet += r0.content.map { it.nickname }
                Log.d(TAG_OFL, "prefetch following=${myFollowingSet.size} followers=${myFollowersSet.size}")
            } catch (e: Exception) {
                Log.w(TAG_OFL, "prefetch failed", e)
            }

            switchTab(currentTab)
        }

        // 무한 스크롤
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (!isLoading && !isLastPage && lastVisible >= total - 3) {
                    when (currentTab) {
                        "followers" -> loadFollowerPage(currentPage + 1, true)
                        else -> loadFollowingPage(currentPage + 1, true)
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

        if (tab == "followers") {
            headerTitle.text = "${targetNickname}\nFollowers"
            indicatorFollowers.visibility = View.VISIBLE
            indicatorFollowing.visibility = View.INVISIBLE
            loadFollowerPage(0, false)
        } else {
            headerTitle.text = "${targetNickname}\nFollowing"
            indicatorFollowing.visibility = View.VISIBLE
            indicatorFollowers.visibility = View.INVISIBLE
            loadFollowingPage(0, false)
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

    private fun applySlice(
        resp: SlicedResponse<GetFollowResponseDto>,
        append: Boolean,
        emptyMsg: String
    ) {
        isLastPage = !resp.hasNext
        currentPage = resp.page

        fun mapToUi(d: GetFollowResponseDto) = UserRowUi(
            nickname = d.nickname,
            profileImgUrl = d.profileImgKey,
            introduction = d.introduction,
            isFollowing = myFollowingSet.contains(d.nickname),
            followsMe = myFollowersSet.contains(d.nickname)
        )

        val newList = resp.content.map(::mapToUi)
        Log.d(TAG_OFL, "applySlice tab=$currentTab page=${resp.page} size=${newList.size} hasNext=${resp.hasNext}")

        if (append) {
            adapter.append(newList)
        } else {
            if (newList.isEmpty()) {
                showEmpty(emptyMsg)
            } else {
                emptyView.visibility = View.GONE
                adapter.submitList(newList)
            }
        }

        currentTotalCount = adapter.itemCount
        headerCount.text = "%,d".format(currentTotalCount)
    }

    private fun loadFollowerPage(page: Int, append: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true
            if (!append) showLoading(true)
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.getFollowerListOf(nickname = targetNickname, page = page, size = 20)
                }
                if (!append) showLoading(false)
                applySlice(resp, append, emptyMsg = "${targetNickname}의 팔로워가 없습니다.")
            } catch (e: Exception) {
                Log.e(TAG_OFL, "loadFollowerPage error p=$page", e)
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
                val resp = withContext(Dispatchers.IO) {
                    api.getFollowingListOf(nickname = targetNickname, page = page, size = 20)
                }
                if (!append) showLoading(false)
                applySlice(resp, append, emptyMsg = "${targetNickname}의 팔로잉이 없습니다.")
            } catch (e: Exception) {
                Log.e(TAG_OFL, "loadFollowingPage error p=$page", e)
                if (!append) {
                    showLoading(false)
                    showEmpty("팔로잉 정보를 불러올 수 없습니다.")
                }
            } finally { isLoading = false }
        }
    }

    private fun toggleFollow(
        targetNickname: String,
        isFollowingNow: Boolean,
        onDone: (Boolean, Boolean) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isFollowingNow) {
                    Network.socialApi(requireContext()).unFollow(targetNickname)
                    myFollowingSet.remove(targetNickname)
                    onDone(true, false)
                } else {
                    Network.socialApi(requireContext()).follow(targetNickname)
                    myFollowingSet.add(targetNickname)
                    onDone(true, true)
                }
            } catch (e: Exception) {
                Log.e(TAG_OFL, "toggleFollow error target=$targetNickname", e)
                onDone(false, isFollowingNow)
            }
        }
    }
    private fun showUnfollowConfirmDialog(
        nickname: String,
        avatarUrl: String?,
        onConfirm: () -> Unit
    ) {
        val v = layoutInflater.inflate(R.layout.dialog_unfollow_confirm, null, false)
        val iv = v.findViewById<ImageView>(R.id.ivProfile)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUnfollow = v.findViewById<MaterialButton>(R.id.btnUnfollow)

        if (!avatarUrl.isNullOrBlank()) {
            iv.load(avatarUrl)
        } else {
            iv.setImageResource(R.drawable.ic_profile_red)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(v)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnUnfollow.setOnClickListener {
            btnUnfollow.isEnabled = false
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }


    // ============== Adapter/Item ==============

    data class UserRowUi(
        val nickname: String,
        val profileImgUrl: String?,
        val introduction: String?,
        var isFollowing: Boolean = false,   // 나 → 그
        var followsMe: Boolean = false      // 그 → 나
    )

    class OtherFollowUserAdapter(
        private val onProfileClick: (UserRowUi) -> Unit,
        private val onFollowToggle: (UserRowUi, Boolean, Int) -> Unit
    ) : RecyclerView.Adapter<OtherFollowUserAdapter.ItemVH>() {

        private val data = mutableListOf<UserRowUi>()
        private var myNickname: String? = null   // 내 닉네임 저장 (내 항목 버튼 숨김)

        fun setMyNickname(nick: String?) {
            myNickname = nick
            notifyDataSetChanged()
            Log.d(TAG_OFL, "adapter.setMyNickname=$myNickname")
        }

        fun submitList(list: List<UserRowUi>) { data.clear(); data.addAll(list); notifyDataSetChanged() }
        fun append(list: List<UserRowUi>) { val s=data.size; data.addAll(list); notifyItemRangeInserted(s, list.size) }
        fun updateFollowState(position: Int, isFollowing: Boolean) {
            if (position in data.indices) {
                data[position].isFollowing = isFollowing
                notifyItemChanged(position)
            }
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
            private val btnUnfollow: ImageView = itemView.findViewById(R.id.btnUnfollow)

            fun bind(u: UserRowUi, pos: Int) {
                val ctx = itemView.context

                if (!u.profileImgUrl.isNullOrBlank()) {
                    avatar.load(u.profileImgUrl) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else avatar.setImageResource(R.drawable.ic_profile_red)

                name.text = u.nickname
                subtitle.text = u.introduction.orEmpty()

                val isSelf = !myNickname.isNullOrBlank() && u.nickname == myNickname
                Log.d(TAG_OFL, "bind pos=$pos user=${u.nickname} isSelf=$isSelf following=${u.isFollowing} followsMe=${u.followsMe}")

                if (isSelf) {
                    // 내 항목이면 버튼 숨김
                    btn.visibility = View.GONE
                    btnUnfollow.visibility = View.GONE
                } else {
                    when {
                        // ✅ 내가 팔로우 중(맞팔 포함) → X 아이콘만
                        u.isFollowing -> {
                            btn.visibility = View.GONE
                            btnUnfollow.visibility = View.VISIBLE
                            btnUnfollow.setImageResource(R.drawable.ic_close)
                            // 문자열 리소스 없으면 하드코딩 가능
                            btnUnfollow.contentDescription = ctx.getString(R.string.unfollow)


                            btnUnfollow.setOnClickListener {
                                Log.d(TAG_OFL, "unfollow click user=${u.nickname} pos=$pos")
                                onFollowToggle(u, true, pos) // 언팔
                            }
                        }
                        // ✅ 그가 나를 팔로우(난 아직) → 맞팔로우 버튼
                        u.followsMe -> {
                            btn.visibility = View.VISIBLE
                            btnUnfollow.visibility = View.GONE
                            btn.text = "맞팔로우"
                            btn.setBackgroundResource(R.drawable.btn_mutual_follow)
                            btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                            btn.setOnClickListener {
                                Log.d(TAG_OFL, "mutual follow click user=${u.nickname} pos=$pos")
                                btn.isEnabled = false
                                onFollowToggle(u, false, pos) // 팔로우
                                btn.isEnabled = true
                            }
                        }
                        // ✅ 서로 팔로우 아님 → 팔로우 버튼
                        else -> {
                            btn.visibility = View.VISIBLE
                            btnUnfollow.visibility = View.GONE
                            btn.text = "팔로우"
                            btn.setBackgroundResource(R.drawable.btn_basic)
                            btn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                            btn.setOnClickListener {
                                Log.d(TAG_OFL, "follow click user=${u.nickname} pos=$pos")
                                btn.isEnabled = false
                                onFollowToggle(u, false, pos) // 팔로우
                                btn.isEnabled = true
                            }
                        }
                    }
                }

                itemView.setOnClickListener {
                    Log.d(TAG_OFL, "row click user=${u.nickname}")
                    onProfileClick(u)
                }
            }
        }
    }
}
