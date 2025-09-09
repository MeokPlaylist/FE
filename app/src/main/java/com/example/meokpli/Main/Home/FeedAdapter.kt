package com.example.meokpli.feed

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
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
import com.example.meokpli.R
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import androidx.viewpager2.widget.ViewPager2


class FeedAdapter(private var items: MutableList<Feed>,
                  private val onCommentClick: (feedId: Long) -> Unit,
                  val onMoreClick: (View, Feed) -> Unit,
                  private val onItemClick: (feedId: Long) -> Unit
    ) : RecyclerView.Adapter<FeedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvUser: TextView = v.findViewById(R.id.tvUserName)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvPageBadge: TextView = v.findViewById(R.id.tvPageBadge)
        val tvCaption: TextView = v.findViewById(R.id.tvCaption)
        val btnLike: ImageView = v.findViewById(R.id.btnLike)
        val btnComment: ImageView = v.findViewById(R.id.btnComment)
        val likeCount: TextView = v.findViewById(R.id.likeCount)
        val commentCount: TextView = v.findViewById(R.id.commentCount)
        val viewPager: androidx.viewpager2.widget.ViewPager2 = v.findViewById(R.id.viewPagerPhotos)
        val btnMore: ImageView = v.findViewById(R.id.btnMore)
    }
    fun addItems(newItems: List<Feed>) {
        val start = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    fun removeItem(feedId: Long) {
        val idx = items.indexOfFirst { it.feedId == feedId }
        if (idx != -1) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
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
        if (item.likeCount != 0.toLong())
            h.likeCount.text = item.likeCount.toString()
        if (item.commentCount != 0.toLong())
            h.commentCount.text = item.commentCount.toString()
        // 댓글 바텀시트
        h.btnComment.setOnClickListener { onCommentClick(item.feedId) }
        h.commentCount.setOnClickListener { onCommentClick(item.feedId) }
        // 점3개 → 피드 액션 바텀시트
        h.btnMore.setOnClickListener { v -> onMoreClick(v, item) }  // ✅ 수정


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
    val content: String?,
    val hashTag: List<String>?,
    val createdAt: String,
    val feedPhotoUrl: List<String>?,
    val likeCount: Long,
    val commentCount: Long
)
