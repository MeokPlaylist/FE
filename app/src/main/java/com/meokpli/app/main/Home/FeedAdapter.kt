package com.meokpli.app.feed

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.meokpli.app.R
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import androidx.viewpager2.widget.ViewPager2
import coil.transform.CircleCropTransformation


class FeedAdapter(private var items: MutableList<Feed>,
                  private val onCommentClick: (feedId: Long) -> Unit,
                  val onMoreClick: (View, Feed) -> Unit,
                  private val onItemClick: (feedId: Long) -> Unit,
                  private val onLocationClick: (feedId: Long, nickName: String) -> Unit,
                  private val onProfileClick: (nickName: String) -> Unit,
                  private val onLikeToggle: (feedId: Long, targetLiked: Boolean, result: (Boolean) -> Unit) -> Unit
) : RecyclerView.Adapter<FeedAdapter.VH>() {

    /** 좋아요 로컬상태(낙관적) */
    data class LikeState(var liked: Boolean, var count: Long)

    /** in-flight 요청 방지용 잠금 */
    private val likeInFlight = mutableSetOf<Long>()

    /** feedId별 로컬 상태 (없으면 item 본래값에서 추정) */
    private val localLike = mutableMapOf<Long, LikeState>()


    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvUser: TextView = v.findViewById(R.id.tvUserName)
        val icProfileImage: ImageView = v.findViewById(R.id.icProfileImage)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvPageBadge: TextView = v.findViewById(R.id.tvPageBadge)
        val tvCaption: TextView = v.findViewById(R.id.tvCaption)
        val btnLike: ImageView = v.findViewById(R.id.btnLike)
        val btnComment: ImageView = v.findViewById(R.id.btnComment)
        val likeCount: TextView = v.findViewById(R.id.likeCount)
        val commentCount: TextView = v.findViewById(R.id.commentCount)
        val viewPager: androidx.viewpager2.widget.ViewPager2 = v.findViewById(R.id.viewPagerPhotos)
        val btnMore: ImageView = v.findViewById(R.id.btnMore)
        val btnLocation: ImageView = v.findViewById(R.id.btnLocation)
    }
    fun addItems(newItems: List<Feed>) {
        val start = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    fun findItem(feedId: Long): Feed? {
        return items.find { it.feedId == feedId }
    }

    fun removeItem(feedId: Long) {
        val idx = items.indexOfFirst { it.feedId == feedId }
        if (idx != -1) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
    /** 상세 등 외부에서 좋아요 변경을 반영하고 싶을 때 사용 */
    fun updateLikeState(feedId: Long, liked: Boolean, count: Long) {
        localLike[feedId] = LikeState(liked, count)
        val idx = items.indexOfFirst { it.feedId == feedId }
        if (idx != -1) {
            items[idx] = items[idx].copy(likeCount = count, isLiked = liked)
            notifyItemChanged(idx, "like")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]

        h.tvUser.text = item.nickName
        h.tvDate.text = item.createdAt
        if (item.commentCount != 0.toLong())
            h.commentCount.text = item.commentCount.toString()
        // 댓글 바텀시트
        h.btnComment.setOnClickListener { onCommentClick(item.feedId) }
        h.commentCount.setOnClickListener { onCommentClick(item.feedId) }
        // 점3개 → 피드 액션 바텀시트
        h.btnMore.setOnClickListener { v -> onMoreClick(v, item) }  // ✅ 수정

        h.btnLocation.setOnClickListener {
            onLocationClick(item.feedId, item.nickName)
        }

        h.icProfileImage.load(item.profileUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_red) // 기본 이미지 (res/drawable/)
            error(R.drawable.ic_profile_red) // 실패 시 표시
            transformations(CircleCropTransformation()) // 동그랗게 자르기
        }

        // 닉네임 및 프로필 이미지 클릭 → 다른 사람 프로필로 이동
        h.tvUser.setOnClickListener {
            onProfileClick(item.nickName)
        }
        h.icProfileImage.setOnClickListener {
            onProfileClick(item.nickName)
        }

        // 내용 + 해시태그
        val tags = item.hashTag?.filter { it.isNotBlank() }?.joinToString(" ") { "#$it" }.orEmpty()
        val fullText =
            if (tags.isBlank()) item.content.orEmpty() else "${item.content.orEmpty()}\n$tags"

        // Spannable 로 해시태그 색칠
        val spannable = SpannableString(fullText)
        val regex = Regex("""#\S+""")
        regex.findAll(fullText).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF0000")),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        h.tvCaption.text = spannable
        var isExpanded = false
        //나중에 더보기 추가
        h.tvCaption.setOnClickListener {
            if (isExpanded) {
                // 접기
                h.tvCaption.maxLines = 3
                h.tvCaption.ellipsize = TextUtils.TruncateAt.END
                isExpanded = false
            } else {
                // 전체 보기
                h.tvCaption.maxLines = Int.MAX_VALUE
                h.tvCaption.ellipsize = null
                isExpanded = true
            }
        }
        // ── 좋아요 초기 상태 세팅 (localLike 우선, 없으면 item 값 사용)
        val st = localLike.getOrPut(item.feedId) { LikeState(item.isLiked, item.likeCount) }
        Log.d("FeedAdapter",item.isLiked.toString())
        applyLikeUi(h, st.liked, st.count)

        // 좋아요 클릭
        h.btnLike.setOnClickListener {
            if (likeInFlight.contains(item.feedId)) return@setOnClickListener

            val prevLiked = st.liked
            val prevCount = st.count
            val targetLiked = !prevLiked
            val newCount = if (targetLiked) prevCount + 1 else maxOf(0, prevCount - 1)

            // 인플라이트 잠금 + 버튼 잠금
            likeInFlight.add(item.feedId)
            h.btnLike.isEnabled = false

            // 낙관적 UI 반영 + 애니메이션
            st.liked = targetLiked
            st.count = newCount
            applyLikeUi(h, st.liked, st.count)
            bounce(h.btnLike)

            // 서버 호출(외부에 위임) → 결과 반영
            onLikeToggle(item.feedId, targetLiked) { success ->
                likeInFlight.remove(item.feedId)
                h.btnLike.isEnabled = true

                if (success) {
                    // 성공 → 원본 아이템 동기화
                    items[pos] = items[pos].copy(likeCount = st.count, isLiked = st.liked)
                    notifyItemChanged(pos, "like")
                } else {
                    // 실패 → 원복
                    st.liked = prevLiked
                    st.count = prevCount
                    applyLikeUi(h, st.liked, st.count)
                }
            }
        }


        // ✅ 여러 장 사진 세팅
        val urls = item.feedPhotoUrl ?: emptyList()
        val indicatorLayout = h.itemView.findViewById<LinearLayout>(R.id.indicatorLayout)

        indicatorLayout.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var lastIndex = 0

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val dotCount = h.viewPager.adapter?.itemCount ?: 0
                if (dotCount <= 1) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v?.isPressed = true   // 눌린 상태 UI 반영
                        startX = event.x
                        lastIndex = h.viewPager.currentItem
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        val threshold = v!!.width.toFloat() / dotCount

                        // 이동 칸 수 계산
                        val offset = (dx / threshold).toInt()
                        var targetIndex = lastIndex + offset
                        if (targetIndex < 0) targetIndex = 0
                        if (targetIndex > dotCount - 1) targetIndex = dotCount - 1

                        if (targetIndex != h.viewPager.currentItem) {
                            h.viewPager.currentItem = targetIndex
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v?.isPressed = false  // 🟢 눌림 해제
                    }
                }
                return true
            }
        })

        if (urls.isNotEmpty()) {
            val photoAdapter = PhotoPagerAdapter(urls)
            h.viewPager.adapter = photoAdapter
            h.viewPager.offscreenPageLimit = 1
            setupIndicator(indicatorLayout, urls.size, 0)
            h.tvPageBadge.text = "1/${urls.size}"
            h.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    h.tvPageBadge.text = "${position + 1}/${urls.size}"
                    updateIndicator(indicatorLayout, position)
                }
            })

            // ✅ 첫 번째 사진은 ViewPager 자체가 즉시 로드
            // ✅ 나머지 사진들은 백그라운드에서 캐싱
            preloadRemainingImages(h.itemView.context, urls.drop(1))
        } else {
            indicatorLayout.visibility = View.GONE
            h.tvPageBadge.text = "0/0"
        }
        // ✅ 댓글 버튼/숫자 클릭 시 BottomSheet 열기
        h.btnComment.setOnClickListener { onCommentClick(item.feedId) }
        h.commentCount.setOnClickListener { onCommentClick(item.feedId) }
        h.viewPager.setOnClickListener {
            onItemClick(item.feedId)
        }
    }


    private fun applyLikeUi(h: VH, liked: Boolean, likeCount: Long) {
        h.btnLike.setImageResource(if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled)
        if (likeCount > 0) {
            h.likeCount.text = likeCount.toString()
            h.likeCount.visibility = View.VISIBLE
        } else {
            h.likeCount.text = ""
            h.likeCount.visibility = View.GONE
        }

    }
    private fun bounce(v: View) {
        v.animate().cancel()
        v.scaleX = 0.9f
        v.scaleY = 0.9f
        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
    }
    private fun preloadRemainingImages(context: Context, urls: List<String>) {
        val imageLoader = context.imageLoader
        urls.forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }

    override fun getItemCount() = items.size
    private fun setupIndicator(layout: LinearLayout, count: Int, currentPos: Int) {
        layout.removeAllViews()

        if (count <= 1) {
            layout.visibility = View.GONE
            return
        } else {
            layout.visibility = View.VISIBLE
        }

        for (i in 0 until count) {
            val dot = View(layout.context).apply {
                val size = when {
                    count <= 5 -> 16 // 5개까지는 모두 같은 크기
                    i == 0 || i == count - 1 -> 8 // 6개 이상일 때 양 끝 점은 작게
                    else -> 16
                }

                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(6, 0, 6, 0)
                }

                background = layout.context.getDrawable(R.drawable.dot_background)
                // 선택 상태 색상은 나중에 업데이트에서 처리
            }
            layout.addView(dot)
        }
        updateIndicator(layout, currentPos)
    }

    private fun updateIndicator(layout: LinearLayout, position: Int) {
        for (i in 0 until layout.childCount) {
            val dot = layout.getChildAt(i)
            if (i == position) {
                dot.background.setTint(Color.parseColor("#FF0000")) // 빨간색
            } else {
                dot.background.setTint(Color.parseColor("#66000000")) // 회색
            }
        }
    }

    class PhotoPagerAdapter(private val urls: List<String>) :
        RecyclerView.Adapter<PhotoPagerAdapter.PhotoVH>() {

        inner class PhotoVH(view: View) : RecyclerView.ViewHolder(view) {
            val img: ImageView = view.findViewById(R.id.iv)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_photo, parent, false)
            return PhotoVH(view)
        }

        override fun onBindViewHolder(holder: PhotoVH, position: Int) {
            val url = urls[position]

            // shimmer placeholder
            val shimmer = Shimmer.AlphaHighlightBuilder()
                .setDuration(1000)
                .setBaseAlpha(0.7f)
                .setHighlightAlpha(0.6f)
                .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build()
            val shimmerDrawable = ShimmerDrawable().apply { setShimmer(shimmer) }

            // Coil 로드
            holder.img.load(url) {
                crossfade(true)
                memoryCachePolicy(CachePolicy.ENABLED)   // 메모리 캐시 사용
                diskCachePolicy(CachePolicy.ENABLED)     // 디스크 캐시 사용
                placeholder(shimmerDrawable)             // 로딩 중 효과
            }
        }

        override fun getItemCount(): Int = urls.size
    }
    fun updateCommentCount(feedId: Long, newCount: Long) {
        val idx = items.indexOfFirst { it.feedId == feedId }
        if (idx != -1) {
            items[idx] = items[idx].copy(commentCount = newCount)
            notifyItemChanged(idx, "comment_count")
        }
    }
}

data class Feed(
    val feedId: Long,
    val nickName: String,
    val profileUrl: String?,
    val content: String?,
    val hashTag: List<String>?,
    val createdAt: String,
    val feedPhotoUrl: List<String>?,
    val likeCount: Long,
    val commentCount: Long,
    var isLiked: Boolean = false //현재 사용자가 눌렀느지?
)
