package com.meokpli.app.main.Home

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.main.CategorySelectDialog
import com.meokpli.app.main.EditContentDialog
import com.meokpli.app.main.MainApi
import com.meokpli.app.main.ModifyFeedCategoryRequest
import com.meokpli.app.main.ModifyFeedContentRequest
import com.meokpli.app.main.ModifyMainFeedPhotoRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException


class FeedDetailActivity : AppCompatActivity() {

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

    private var feedId: Long = 0L

    // 소유자 판별용
    private var myNickname: String? = null
    private var feedAuthorNickname: String? = null


    // 상세에서 가져온 현재 선택(있으면 프리셋)
    private var currentCategories: List<String> = emptyList()
    private var currentRegions: ArrayList<String> = arrayListOf()

    private var isLikedByMe: Boolean = false
    private var likeCount: Long = 0
    private var commentCount: Long = 0

    private fun isMineNow(): Boolean =
        !myNickname.isNullOrBlank() && !feedAuthorNickname.isNullOrBlank() &&
                (myNickname == feedAuthorNickname)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_detail)

        feedId = intent.getLongExtra("feedId", 0L)

        tvUserName = findViewById(R.id.tvUserName)
        imgAvatar = findViewById(R.id.imgAvatar)
        tvDate = findViewById(R.id.tvDate)
        btnMore = findViewById(R.id.btnMore)
        chipGroup = findViewById(R.id.chipGroup)
        viewPager = findViewById(R.id.viewPagerPhotos)
        tvCaption = findViewById(R.id.tvCaption)
        btnComment = findViewById(R.id.btnComment)
        tvPageBadge = findViewById(R.id.tvPageBadge)
        ivLike = findViewById(R.id.ivLike)
        tvLikeCount = findViewById(R.id.tvLikeCount)
        tvCommentCount = findViewById(R.id.tvCommentCount)

        // 내 닉네임 로드
        lifecycleScope.launch {
            runCatching { Network.userApi(this@FeedDetailActivity).getMyPage() }
                .onSuccess { myNickname = it.userNickname }
                .onFailure { myNickname = null }
        }

        // 상세 로드
        loadFeedDetail(feedId)

        // 점 3개
        btnMore.setOnClickListener { v ->
            if (isMineNow()) {
                FeedActionsBottomSheet.newInstance(feedId)
                    .show(supportFragmentManager, "feed_actions")
            } else {
                showReportPopup(v)
            }
        }

        // 댓글
        btnComment.setOnClickListener {
            CommentsBottomSheet.newInstance(feedId)
                .show(supportFragmentManager, "comments")
        }
        //댓글수 변경
        supportFragmentManager.setFragmentResultListener(
            "comment_result", this
        ) { _, bundle ->
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
        // 액션 바텀시트 결과
        private fun bindActionSheetResults() {
        supportFragmentManager.setFragmentResultListener(
            FeedActionsBottomSheet.REQUEST_KEY, this
        ) { _, bundle ->
            val action = bundle.getString(FeedActionsBottomSheet.KEY_ACTION) ?: return@setFragmentResultListener
            val feedIdArg = bundle.getLong(FeedActionsBottomSheet.KEY_FEED_ID, 0L)
            if (feedIdArg == 0L) return@setFragmentResultListener

            when (action) {
                FeedActionsBottomSheet.ACTION_EDIT_COVER -> {
                    val currentIndex = viewPager.currentItem
                    val images = (viewPager.adapter as? PhotoPagerAdapter)?.images ?: emptyList()
                    FeedCoverPickBottomSheet.newInstance(
                        feedId = feedIdArg,
                        images = ArrayList(images),
                        currentMainIndex = currentIndex
                    ).show(supportFragmentManager, "cover_pick")
                }
                FeedActionsBottomSheet.ACTION_EDIT_CATEGORY -> {
                    // ⬇️ 현재 다이얼로그 시그니처에 맞게 파라미터명 조정 (preComps)
                    CategorySelectDialog.newInstance(
                        preMoods = arrayListOf<String>(),
                        preFoods = arrayListOf<String>(),
                        preComps = arrayListOf<String>() // ← preCompanions 아님!
                        // preRegions 파라미터 없음 (현 다이얼로그 버전)
                    ).show(supportFragmentManager, "category_select_first")
                }
                FeedActionsBottomSheet.ACTION_EDIT_POST -> {
                    val initial = tvCaption.text?.toString().orEmpty()
                    EditContentDialog.newInstance(feedIdArg, initial)
                        .show(supportFragmentManager, "edit_content")
                }

                FeedActionsBottomSheet.ACTION_DELETE -> {
                    confirmDeleteInDetail(feedIdArg)
                }
            }
        }

        // 카테고리 다이얼로그 최종 결과 → 서버 전송
        supportFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, this
        ) { _, b ->
            // 1) 분리 키가 있으면 우선 사용
            val moods = b.getStringArrayList(CategorySelectDialog.KEY_MOODS) ?: arrayListOf()
            val foods = b.getStringArrayList(CategorySelectDialog.KEY_FOODS) ?: arrayListOf()
            val comps = b.getStringArrayList(CategorySelectDialog.KEY_COMPANIONS) ?: arrayListOf()
            var categories = (moods + foods + comps).distinct()

            // 2) 지역과(필요시) 카테고리를 payload에서 파싱 (현 다이얼로그는 payload 제공)
            val payload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD) ?: arrayListOf()
            if (categories.isEmpty()) {
                categories = payload.filter {
                    it.startsWith("moods:") || it.startsWith("foods:") || it.startsWith("companions:")
                }.map { it.substringAfter(':') }.distinct()
            }
            val regions: List<String> = payload.filter { it.startsWith("regions:") }
                .map { it.substringAfter("regions:").replace('|', ':') } // "서울|강남구" → "서울:강남구"

            lifecycleScope.launch {
                try {
                    val resp = Network.feedApi(this@FeedDetailActivity).modifyFeedCategory(
                        ModifyFeedCategoryRequest(
                            feedId = feedId,
                            categories = categories,
                            regions = regions // 없으면 빈 배열 전송 OK
                        )
                    )
                    if (resp.isSuccessful) {
                        Toast.makeText(this@FeedDetailActivity, "카테고리/지역이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        currentRegions = ArrayList(regions)
                        currentCategories = categories
                    } else {
                        Toast.makeText(this@FeedDetailActivity, "실패: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FeedDetailActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 대표사진 선택 결과
        supportFragmentManager.setFragmentResultListener(
            FeedCoverPickBottomSheet.REQUEST_KEY, this
        ) { _, bundle ->
            val newIndex = bundle.getInt(FeedCoverPickBottomSheet.KEY_NEW_INDEX, -1)
            val oldIndex = bundle.getInt(FeedCoverPickBottomSheet.KEY_OLD_INDEX, -1)
            val feedIdArg = bundle.getLong(FeedCoverPickBottomSheet.KEY_FEED_ID, 0L)
            if (newIndex < 0 || feedIdArg == 0L) return@setFragmentResultListener

            lifecycleScope.launch {
                try {
                    val res = Network.feedApi(this@FeedDetailActivity)
                        .modifyMainFeedPhoto(
                            ModifyMainFeedPhotoRequest(
                                feedId = feedIdArg,
                                newMainFeedPhotoSequence = newIndex,
                                oldMainFeedPhotoSequence = if (oldIndex >= 0) oldIndex else 0
                            )
                        )
                    if (res.isAvailable) {
                        viewPager.setCurrentItem(newIndex, true)
                        Toast.makeText(this@FeedDetailActivity, "대표사진이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FeedDetailActivity, "대표사진 변경 실패", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: HttpException) {
                    Toast.makeText(this@FeedDetailActivity, "오류: ${e.code()}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@FeedDetailActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // ACTION 처리부에 "게시글 수정" 연결
        supportFragmentManager.setFragmentResultListener(
            FeedActionsBottomSheet.REQUEST_KEY, this
        ) { _, bundle ->
            val action = bundle.getString(FeedActionsBottomSheet.KEY_ACTION) ?: return@setFragmentResultListener
            val feedIdArg = bundle.getLong(FeedActionsBottomSheet.KEY_FEED_ID, 0L)
            if (feedIdArg == 0L) return@setFragmentResultListener

            when (action) {
                FeedActionsBottomSheet.ACTION_EDIT_POST -> {
                    val initial = tvCaption.text?.toString().orEmpty()
                    EditContentDialog.newInstance(feedIdArg, initial)
                        .show(supportFragmentManager, "edit_content")
                }
                // …기존 ACTION_EDIT_COVER / ACTION_EDIT_CATEGORY 처리 유지…
            }
        }

// 다이얼로그 결과 수신 → 서버 호출 → UI 갱신
        supportFragmentManager.setFragmentResultListener(
            EditContentDialog.REQUEST_KEY, this
        ) { _, b ->
            val id = b.getLong(EditContentDialog.KEY_FEED_ID, 0L)
            val newContent =
                b.getString(EditContentDialog.KEY_NEW_CONTENT) ?: return@setFragmentResultListener
            if (id == 0L) return@setFragmentResultListener

            lifecycleScope.launch {
                try {
                    val ok = Network.feedApi(this@FeedDetailActivity)
                        .modifyFeedContent(ModifyFeedContentRequest(id, newContent))
                    if (ok.isSuccessful) {
                        tvCaption.text = newContent
                        Toast.makeText(this@FeedDetailActivity, "글이 수정되었습니다.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            this@FeedDetailActivity,
                            "수정 실패: ${ok.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FeedDetailActivity, "오류: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFeedDetail(feedId: Long) {
        lifecycleScope.launch {
            try {
                val api: MainApi = Network.feedApi(this@FeedDetailActivity)
                val resp = api.getFeedDetail(feedId).detailInforDto

                feedAuthorNickname = resp.nickName
                tvUserName.text = resp.nickName
                tvDate.text = resp.createdAt
                tvCaption.text = resp.content
                //프로필 완성되면 손봐야함
                imgAvatar.setImageResource(R.drawable.ic_profile_red)

                isLikedByMe = resp.feedLike
                likeCount = resp.likeCount
                commentCount = resp.commentCount
                renderLikeUi()

                tvCommentCount.text = commentCount.toString()




                chipGroup.removeAllViews()
                resp.hashTag.forEach { tag ->
                    val chip = Chip(this@FeedDetailActivity).apply {
                        text = "#$tag"; isClickable = false
                    }
                    chipGroup.addView(chip)
                }

                val urls = resp.feedPhotoUrl
                val pagerAdapter = PhotoPagerAdapter(urls)
                viewPager.adapter = pagerAdapter

                tvPageBadge.text = if (urls.isEmpty()) "0/0" else "1/${urls.size}"
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        tvPageBadge.text = "${position + 1}/${urls.size}"
                    }
                })

                // 프리셋이 필요하면 서버 응답에 맞춰 여기서 currentCategories/currentRegions 세팅

            } catch (e: Exception) {
                Toast.makeText(this@FeedDetailActivity, "상세 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 좋아요 토글 → 서버 전송 → UI 즉시 반영(낙관적) */
    private fun toggleLike() {
        val prevLiked = isLikedByMe
        val prevCount = likeCount

        // 낙관적 업데이트
        isLikedByMe = !prevLiked
        likeCount = if (isLikedByMe) prevCount + 1 else (prevCount - 1).coerceAtLeast(0)
        renderLikeUi()

        lifecycleScope.launch {
            try {
                val api = Network.feedApi(this@FeedDetailActivity)
                if (isLikedByMe) {
                    val r = api.likeFeed(feedId)
                    if (!r.isSuccessful) throw HttpException(r)
                } else {
                    val r = api.unlikeFeed(feedId)
                    if (!r.isSuccessful) throw HttpException(r)
                }
            } catch (t: Throwable) {
                // 실패 시 롤백
                isLikedByMe = prevLiked
                likeCount   = prevCount
                renderLikeUi()
                Toast.makeText(this@FeedDetailActivity, "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderLikeUi() {
        ivLike.setImageResource(if (isLikedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled)
        tvLikeCount.text = likeCount.toString()
    }

    private fun confirmDeleteInDetail(feedId: Long) {
        AlertDialog.Builder(this)
            .setTitle("게시글을 삭제할까요?")
            .setMessage("한번 삭제한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { d, _ ->
                lifecycleScope.launch {
                    try {
                        val res = Network.feedApi(this@FeedDetailActivity).deleteFeed(feedId)
                        if (res.isSuccessful) {
                            Toast.makeText(this@FeedDetailActivity, "삭제되었습니다.", Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@FeedDetailActivity,
                                "삭제 실패: ${res.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: HttpException) {
                        Toast.makeText(
                            this@FeedDetailActivity,
                            "삭제 실패: ${e.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FeedDetailActivity,
                            "삭제 실패: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                d.dismiss()
            }
            .show()

    }

    private fun showReportPopup(anchor: View) {
        val view = LayoutInflater.from(this).inflate(R.layout.popup_feed_report, null, false)
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
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = view.measuredWidth
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }

    private fun showReportConfirmDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("정말로 신고하시겠습니까?")
            .setMessage("한번 신고한 게시물은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("신고") { d, _ ->
                lifecycleScope.launch {
                    try {
                        val api = Network.feedApi(this@FeedDetailActivity)
                        api.reportFeed(feedId)
                        Toast.makeText(this@FeedDetailActivity, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FeedDetailActivity, "신고 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                d.dismiss()
            }
            .show()
    }

}
