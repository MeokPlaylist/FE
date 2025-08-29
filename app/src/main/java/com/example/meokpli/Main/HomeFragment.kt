package com.example.meokpli.Main

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Auth.Network
import com.example.meokpli.R
import com.example.meokpli.feed.Feed
import com.example.meokpli.feed.FeedAdapter
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var feedApi: MainApi
    private lateinit var rv: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedApi = Network.feedApi(requireContext())

        rv = view.findViewById(R.id.recyclerFeed)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 처음엔 빈 리스트로 어댑터 붙여두기(깜빡임/스냅샷 방지)
        rv.adapter = FeedAdapter(emptyList())

        // 실제 데이터 로드
        loadFeed()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sliced = feedApi.getMainFeeds()
                val body = sliced.content ?: emptyList()

                // ✅ URL들을 미리 preload
                val urls = body.mapNotNull { it.feedPhotoUrl?.firstOrNull() }
                preloadImages(urls)

                val items: List<Feed> = body.map { dto ->
                    Feed(
                        nickName = dto.nickName,
                        createdAt = formatKST(dto.createdAt),
                        content = dto.content ?: "",
                        hashTag = dto.hashTag ?: emptyList(),
                        feedPhotoUrl = dto.feedPhotoUrl ?: emptyList(),
                        likeCount = dto.likeCount,
                        commentCount = dto.commentCount
                    )
                }

                rv.adapter = FeedAdapter(items)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun preloadImages(urls: List<String>) {
        val loader = coil.ImageLoader(requireContext())
        urls.forEach { url ->
            val request = coil.request.ImageRequest.Builder(requireContext())
                .data(url)
                .allowHardware(false)
                .build()
            loader.enqueue(request) // 캐시에 저장
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatKST(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val odt = OffsetDateTime.parse(iso)           // 예: "2025-08-23T12:24:40+09:00"
            val seoul = odt.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
            seoul.format(DateTimeFormatter.ofPattern("yyyy.M.d"))
        } catch (_: Throwable) {
            // 서버가 "2025-08-23T12:24:40" 식이면 Offset 없는 LocalDateTime일 수 있음
            try {
                val ldt = java.time.LocalDateTime.parse(iso)
                ldt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy.M.d"))
            } catch (_: Throwable) { "" }
        }
    }

    private fun buildCaption(content: String?, hashtags: List<String>?): String {
        val base = content.orEmpty()
        val tags = hashtags.orEmpty().filter { it.isNotBlank() }.joinToString(" ") { "#$it" }
        return if (tags.isBlank()) base else {
            if (base.isBlank()) tags else "$base\n$tags"
        }
    }
}
