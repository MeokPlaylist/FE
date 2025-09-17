package com.meokpli.app.main.Home


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import com.meokpli.app.auth.Network
import com.meokpli.app.main.MainApi
import com.meokpli.app.main.Resettable
import com.meokpli.app.R
import com.meokpli.app.comments.CommentsBottomSheet
import com.meokpli.app.feed.Feed
import com.meokpli.app.feed.FeedAdapter
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.app.AlertDialog
import com.meokpli.app.main.Home.FeedDetailActivity

class HomeFragment : Fragment(R.layout.fragment_home), Resettable {

    private lateinit var feedApi: MainApi
    private lateinit var rv: RecyclerView
    private lateinit var adapter: FeedAdapter

    private var isLoading = false
    private var hasNext = true
    private var currentPage = 0
    private val pageSize = 10
    private var myNickname: String? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedApi = Network.feedApi(requireContext())


        // 내 닉네임 1회 로드
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { Network.userApi(requireContext()).getPersonalInfo() }
                .onSuccess { myNickname = it.name }
                .onFailure { myNickname = null }
        }

        rv = view.findViewById(R.id.recyclerFeed)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 처음엔 빈 리스트로 어댑터 붙여두기(깜빡임/스냅샷 방지)
        // ✅ 콜백 포함한 생성자로 교체
        // 어댑터 생성부 교체
        adapter = FeedAdapter(
            mutableListOf(),
            onCommentClick = { feedId ->
                CommentsBottomSheet.newInstance(feedId)
                    .show(childFragmentManager, "comments")
            },
            onMoreClick = { anchor, item ->
                val isMine = !myNickname.isNullOrBlank() && (myNickname == item.nickName)
                if (isMine) showMyFeedPopup(anchor, item.feedId)
                else showReportPopup(anchor, item.feedId)
            },
            onItemClick = { feedId ->
                Toast.makeText(requireContext(),
                    "홈에서는 상세보기를 지원하지 않아요.\n프로필 또는 검색 결과에서 열어주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        rv.adapter = adapter

        // ✅ BottomSheet에서 댓글 수 갱신 전달 받기
        childFragmentManager.setFragmentResultListener("comment_result", viewLifecycleOwner) { _, bundle ->
            val feedId = bundle.getLong("feedId")
            val newCount = bundle.getInt("count").toLong()
            if (feedId != 0L) {
                adapter.updateCommentCount(feedId, newCount)
            }
        }


        // 실제 데이터 로드
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lm = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val totalCount = lm.itemCount

                if (!isLoading && hasNext && lastVisible >= totalCount - 3) {
                    loadFeed()
                }
            }
        })

        loadFeed()
    }
    override fun resetToDefault() {
        rv.scrollToPosition(0) // 맨 위로 스크롤
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFeed() {
        isLoading = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sliced = feedApi.getMainFeeds(currentPage, pageSize)
                val body = sliced.content ?: emptyList()

                val items = body.map { dto ->
                    Feed(
                        feedId = dto.feedId,
                        nickName = dto.nickName,
                        createdAt = formatKST(dto.createdAt),
                        content = dto.content ?: "",
                        hashTag = dto.hashTag ?: emptyList(),
                        feedPhotoUrl = dto.feedPhotoUrl ?: emptyList(),
                        likeCount = dto.likeCount,
                        commentCount = dto.commentCount
                    )
                }

                adapter.addItems(items)
                hasNext = sliced.hasNext
                if (hasNext) currentPage++

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }
    private fun preloadImages(urls: List<String>) {
        val loader = ImageLoader(requireContext())
        urls.forEach { url ->
            val request = ImageRequest.Builder(requireContext())
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
                val ldt = LocalDateTime.parse(iso)
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
    /** 내 피드 팝업 */
    private fun showMyFeedPopup(anchor: View, feedId: Long) {
        val v = LayoutInflater.from(anchor.context)
            .inflate(R.layout.popup_feed_actions_home, null, false)

        val popup = PopupWindow(
            v,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        v.findViewById<TextView>(R.id.itemEditPost).setOnClickListener {
            popup.dismiss()
            // TODO: 글 수정 화면 이동 (임시로 상세로 이동)
            openDetail(feedId)
        }
        v.findViewById<TextView>(R.id.itemEditCover).setOnClickListener {
            popup.dismiss()
            // 대표사진 변경은 상세에서(이미지 컨텍스트가 상세에 있음)
            openDetail(feedId)
        }
        v.findViewById<TextView>(R.id.itemEditCategory).setOnClickListener {
            popup.dismiss()
            // TODO: 카테고리 수정 화면/바텀시트 (여기도 상세로 임시 이동)
            openDetail(feedId)
        }
        v.findViewById<TextView>(R.id.itemDelete).setOnClickListener {
            popup.dismiss()
            // TODO: 삭제 흐름(확인 → API → 목록 갱신)
            confirmDelete(feedId)
        }

        showPopupBelowRight(popup, anchor, v)
    }

    /** 남의 피드 팝업(신고 1개) */
    private fun showReportPopup(anchor: View, feedId: Long) {
        val v = LayoutInflater.from(anchor.context)
            .inflate(R.layout.popup_feed_report, null, false)

        val popup = PopupWindow(
            v,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        v.findViewById<TextView>(R.id.tvReport).setOnClickListener {
            popup.dismiss()
            showReportConfirm(feedId)
        }

        showPopupBelowRight(popup, anchor, v)
    }

    /** 공통: anchor 우측 정렬로 아래 표시 */
    private fun showPopupBelowRight(popup: PopupWindow, anchor: View, contentView: View) {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = contentView.measuredWidth
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }

    private fun showReportConfirm(feedId: Long) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("정말로 신고하시겠습니까?")
            .setMessage("한번 신고한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("신고") { d, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        Network.feedApi(requireContext()).reportFeed(feedId)
                        Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (e: HttpException) {
                        Toast.makeText(requireContext(), "신고 실패: ${e.code()}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "신고 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                d.dismiss()
            }
            .show()
    }

    private fun openDetail(feedId: Long) {
        val intent = Intent(requireContext(), FeedDetailActivity::class.java)
        intent.putExtra("feedId", feedId)
        startActivity(intent)
    }

    /** 삭제 확인 다이얼로그 → 서버 호출 → 리스트에서 아이템 제거 */
    private fun confirmDelete(feedId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("게시글을 삭제할까요?")
            .setMessage("한번 삭제한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { d, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val res = Network.feedApi(requireContext()).deleteFeed(feedId)
                        if (res.isSuccessful) {
                            adapter.removeItem(feedId) // ← 어댑터에 아래 메서드 추가 필요
                            Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "삭제 실패: ${res.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: HttpException) {
                        Toast.makeText(requireContext(), "삭제 실패: ${e.code()}", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                d.dismiss()
            }
            .show()

    }
}