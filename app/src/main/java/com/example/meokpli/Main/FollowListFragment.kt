package com.example.meokpli.Main

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class FollowListFragment : Fragment() {

    enum class FollowTab { FOLLOWERS, FOLLOWING }

    companion object {
        private const val ARG_TAB = "arg_tab"
        private const val ARG_FOLLOWERS = "arg_followers"
        private const val ARG_FOLLOWING = "arg_following"

        fun newInstance(
            initialTab: FollowTab,
            followersCount: Int = 0,
            followingCount: Int = 0
        ) = FollowListFragment().apply {
            arguments = bundleOf(
                ARG_TAB to initialTab.name,
                ARG_FOLLOWERS to followersCount,
                ARG_FOLLOWING to followingCount
            )
        }
    }

    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowing: TextView
    private lateinit var indicatorFollowers: View
    private lateinit var indicatorFollowing: View
    private lateinit var headerTitle: TextView
    private lateinit var headerCount: TextView
    private lateinit var recycler: RecyclerView

    private var currentTab = FollowTab.FOLLOWING
    private var followersCount = 0
    private var followingCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_follow_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back
        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack() // 상세 화면이므로 백스택에서 빠져나감
        }

        tabFollowers = view.findViewById(R.id.tabFollowers)
        tabFollowing = view.findViewById(R.id.tabFollowing)
        indicatorFollowers = view.findViewById(R.id.indicatorFollowers)
        indicatorFollowing = view.findViewById(R.id.indicatorFollowing)
        headerTitle = view.findViewById(R.id.headerTitle)
        headerCount = view.findViewById(R.id.headerCount)
        recycler = view.findViewById(R.id.recycler)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = SimpleUserAdapter(emptyList()) // 우선 빈 어댑터

        // args
        currentTab = arguments?.getString(ARG_TAB)?.let { FollowTab.valueOf(it) } ?: FollowTab.FOLLOWING
        followersCount = arguments?.getInt(ARG_FOLLOWERS) ?: 0
        followingCount = arguments?.getInt(ARG_FOLLOWING) ?: 0

        // 클릭
        view.findViewById<View>(R.id.boxFollowers).setOnClickListener {
            switchTab(FollowTab.FOLLOWERS)
        }
        view.findViewById<View>(R.id.boxFollowing).setOnClickListener {
            switchTab(FollowTab.FOLLOWING)
        }

        // 초기 표시
        switchTab(currentTab)
    }

    private fun switchTab(tab: FollowTab) {
        currentTab = tab

        val active = Color.BLACK
        val inactive = Color.parseColor("#E0E0E0")

        tabFollowers.setTextColor(if (tab == FollowTab.FOLLOWERS) active else inactive)
        tabFollowing.setTextColor(if (tab == FollowTab.FOLLOWING) active else inactive)
        indicatorFollowers.visibility = if (tab == FollowTab.FOLLOWERS) View.VISIBLE else View.INVISIBLE
        indicatorFollowing.visibility = if (tab == FollowTab.FOLLOWING) View.VISIBLE else View.INVISIBLE

        if (tab == FollowTab.FOLLOWERS) {
            headerTitle.text = "My\nFollowers"
            headerCount.text = "%,d".format(followersCount)
            // TODO: 서버에서 followers 목록 로딩 → recycler.adapter.submitList(...)
        } else {
            headerTitle.text = "My\nFollowing"
            headerCount.text = "%,d".format(followingCount)
            // TODO: 서버에서 following 목록 로딩 → recycler.adapter.submitList(...)
        }
    }

    /** 아주 단순한 placeholder 어댑터 (경고 방지용) */
    private class SimpleUserAdapter(
        private var items: List<String>
    ) : RecyclerView.Adapter<SimpleUserAdapter.VH>() {

        class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 16f
                setPadding(8, 16, 8, 16)
            }
            return VH(tv)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = items[position]
        }
        override fun getItemCount() = items.size

        fun submit(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }
    }
}
