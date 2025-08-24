package com.example.meokpli.Main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.example.meokpli.feed.Feed
import com.example.meokpli.feed.FeedAdapter

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.recyclerFeed)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 딱 2개만 표시
        val items = listOf(
            Feed(
                userName = "김수민",
                date = "2025.9.24",
                caption = "안녕하십니까, 디자인 감다훼 김수민입니다. 잘부탁드립니다.\n너무 힘들어요",
                imageRes = R.drawable.ic_profile_camera
            ),
            Feed(
                userName = "김수민",
                date = "2025.9.24",
                caption = "두 번째 피드 예시입니다.",
                imageRes = R.drawable.ic_profile_camera
            )
        )

        rv.adapter = FeedAdapter(items)
    }
}
