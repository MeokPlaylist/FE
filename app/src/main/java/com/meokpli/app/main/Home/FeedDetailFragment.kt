package com.meokpli.app.main.Home

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.chip.ChipGroup
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.main.*
import kotlinx.coroutines.launch

class FeedDetailFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var imgAvatar: ImageView
    private lateinit var tvDate: TextView
    private lateinit var btnMore: ImageButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var viewPager: ViewPager2
    private lateinit var tvCaption: TextView
    private lateinit var btnComment: ImageView
    private lateinit var tvPageBadge: TextView
    private lateinit var ivLike: ImageView
    private lateinit var tvLikeCount: TextView
    private lateinit var tvCommentCount: TextView
    private lateinit var btnRoadmap: ImageView

    private var feedId: Long = 0L

    private var myNickname: String? = null
    private var feedAuthorNickname: String? = null
    private var photoUrls: List<String> = emptyList()

    private var currentCategories: List<String> = emptyList()
    private var currentRegions: ArrayList<String> = arrayListOf()

    private var isLikedByMe: Boolean = false
    private var likeCount: Long = 0
    private var commentCount: Long = 0

    private fun isMineNow(): Boolean =
        !myNickname.isNullOrBlank() && !feedAuthorNickname.isNullOrBlank() &&
                (myNickname == feedAuthorNickname)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feed_detail, container, false)
        // 기존 layout 재사용 (원래 Activity의 레이아웃)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // argument로부터 feedId 수신
        feedId = arguments?.getLong("feedId") ?: 0L

        // View 바인딩
        tvUserName = view.findViewById(R.id.tvUserName)
        imgAvatar = view.findViewById(R.id.imgAvatar)
        btnRoadmap = view.findViewById(R.id.btnLocation)
        tvDate = view.findViewById(R.id.tvDate)
        btnMore = view.findViewById(R.id.btnMore)
        chipGroup = view.findViewById(R.id.chipGroup)
        viewPager = view.findViewById(R.id.viewPagerPhotos)
        tvCaption = view.findViewById(R.id.tvCaption)
        btnComment = view.findViewById(R.id.btnComment)
        tvPageBadge = view.findViewById(R.id.tvPageBadge)
        ivLike = view.findViewById(R.id.ivLike)
        tvLikeCount = view.findViewById(R.id.tvLikeCount)
        tvCommentCount = view.findViewById(R.id.tvCommentCount)

        // 내 닉네임 로드
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { Network.userApi(requireContext()).getMyPage() }
                .onSuccess { myNickname = it.userNickname }
                .onFailure { myNickname = null }
        }

        // 피드 상세 로드
        loadFeedDetail(feedId)

        // 점 3개 메뉴
        btnMore.setOnClickListener { v ->
            if (isMineNow()) {
                FeedActionsBottomSheet.newInstance(feedId)
                    .show(parentFragmentManager, "feed_actions")
            } else {
                showReportPopup(v)
            }
        }

        btnRoadmap.setOnClickListener {
            val bundle = Bundle().apply { putLong("feedId", feedId) }
            findNavController().navigate(R.id.roadmapView, bundle)
        }

        // 댓글 바텀시트 열기
        btnComment.setOnClickListener {
            CommentsBottomSheet.newInstance(feedId)
                .show(parentFragmentManager, "comments")
        }

        // 댓글 개수 업데이트 리스너
        parentFragmentManager.setFragmentResultListener("comment_result", viewLifecycleOwner) { _, bundle ->
            val id = bundle.getLong("feedId", 0L)
            val newCount = bundle.getInt("count", -1)
            if (id == feedId && newCount >= 0) {
                commentCount = newCount.toLong()
                tvCommentCount.text = commentCount.toString()
            }
        }

        ivLike.setOnClickListener { toggleLike() }

        bindActionSheetResults()
    }

    // FeedActionsBottomSheet 결과 수신
    private fun bindActionSheetResults() {
        parentFragmentManager.setFragmentResultListener(
            FeedActionsBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString(FeedActionsBottomSheet.KEY_ACTION) ?: return@setFragmentResultListener
            val feedIdArg = bundle.getLong(FeedActionsBottomSheet.KEY_FEED_ID, 0L)
            if (feedIdArg == 0L) return@setFragmentResultListener

            when (action) {
                FeedActionsBottomSheet.ACTION_EDIT_POST -> {
                    val initial = tvCaption.text?.toString().orEmpty()
                    EditContentDialog.newInstance(feedIdArg, initial)
                        .show(parentFragmentManager, "edit_content")
                }
                FeedActionsBottomSheet.ACTION_DELETE -> confirmDeleteInDetail(feedIdArg)
                FeedActionsBottomSheet.ACTION_EDIT_CATEGORY -> {
                    CategorySelectDialog.newInstance(
                        preMoods = ArrayList(currentCategories),
                        preFoods = arrayListOf(),
                        preComps = arrayListOf(),
                        preRegions = ArrayList(currentRegions)
                    ).show(parentFragmentManager, "category_select")
                }
            }
        }

        // 글 수정 결과 리스너
        parentFragmentManager.setFragmentResultListener(
            EditContentDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            val id = b.getLong(EditContentDialog.KEY_FEED_ID, 0L)
            val newContent = b.getString(EditContentDialog.KEY_NEW_CONTENT) ?: return@setFragmentResultListener
            if (id == 0L) return@setFragmentResultListener

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val ok = Network.feedApi(requireContext())
                        .modifyFeedContent(ModifyFeedContentRequest(id, newContent))
                    if (ok.isSuccessful) {
                        tvCaption.text = newContent
                        Toast.makeText(requireContext(), "글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "수정 실패: ${ok.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFeedDetail(feedId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = Network.feedApi(requireContext())
                val resp = api.getFeedDetail(feedId).detailInforDto

                feedAuthorNickname = resp.nickName
                tvUserName.text = resp.nickName
                tvDate.text = relativeTimeKST(resp.createdAt)
                tvCaption.text = resp.content

                imgAvatar.load(resp.profileUrl) {
                    placeholder(R.drawable.ic_profile_red)
                    error(R.drawable.ic_profile_red)
                }

                photoUrls = resp.feedPhotoUrl ?: emptyList()
                val pagerAdapter = PhotoPagerAdapter(photoUrls)
                viewPager.adapter = pagerAdapter

                tvPageBadge.text = if (photoUrls.isEmpty()) "0/0" else "1/${photoUrls.size}"
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        tvPageBadge.text = "${position + 1}/${photoUrls.size}"
                    }
                })

                isLikedByMe = resp.feedLike
                likeCount = resp.likeCount
                commentCount = resp.commentCount
                renderLikeUi()
                tvCommentCount.text = commentCount.toString()

                val displayTokens: List<String> = resp.feedCategories ?: emptyList()
                currentCategories = displayTokens.filterNot { it.contains(":") }
                currentRegions = ArrayList(displayTokens.filter { it.contains(":") })
                renderCategoryChips(displayTokens)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "상세 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderCategoryChips(list: List<String>) {
        chipGroup.removeAllViews()
        list.forEach { token ->
            val chip = layoutInflater.inflate(R.layout.item_chip, chipGroup, false)
            chip.findViewById<TextView>(R.id.chipText).apply {
                text = toDisplayLabel(token)
            }
            chip.findViewById<ImageView>(R.id.chipClose).visibility = View.GONE
            chipGroup.addView(chip)
        }
    }

    private fun toDisplayLabel(token: String): String {
        return if (token.contains(":"))
            CategoryLabels.regionToKorean(token).replace(":", " ")  // 콜론 → 공백
        else
            CategoryLabels.toKorean(token)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun relativeTimeKST(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val parsed = java.time.OffsetDateTime.parse(iso)
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
            val now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
            val minutes = java.time.Duration.between(parsed, now).toMinutes()
            when {
                minutes < 60 -> "${minutes}분 전"
                minutes < 60 * 24 -> "${minutes / 60}시간 전"
                else -> parsed.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
        } catch (_: Throwable) { iso }
    }

    private fun renderLikeUi() {
        ivLike.setImageResource(if (isLikedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled)
        tvLikeCount.text = likeCount.toString()
    }

    private fun toggleLike() = viewLifecycleOwner.lifecycleScope.launch {
        try {
            val api = Network.socialApi(requireContext())
            val res = if (isLikedByMe) api.feedUnLike(feedId) else api.feedLike(feedId)
            if (res.isSuccessful) {
                isLikedByMe = !isLikedByMe
                likeCount = (likeCount + if (isLikedByMe) 1 else -1).coerceAtLeast(0)
                renderLikeUi()
            } else {
                Toast.makeText(requireContext(), "좋아요 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "좋아요 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteInDetail(feedId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("게시글을 삭제할까요?")
            .setMessage("한번 삭제한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { d, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val res = Network.feedApi(requireContext()).deleteFeed(feedId)
                        if (res.isSuccessful) {
                            Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        } else {
                            Toast.makeText(requireContext(), "삭제 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                d.dismiss()
            }.show()
    }

    private fun showReportPopup(anchor: View) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.popup_feed_report, null, false)
        val popup = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        view.findViewById<TextView>(R.id.tvReport).setOnClickListener {
            popup.dismiss(); showReportConfirmDialog()
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val popupW = view.measuredWidth
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }

    private fun showReportConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("정말로 신고하시겠습니까?")
            .setMessage("한번 신고한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("신고") { d, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val api = Network.feedApi(requireContext())
                        api.reportFeed(feedId)
                        Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "신고 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                d.dismiss()
            }.show()
    }
}
