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
import coil.transform.CircleCropTransformation
import com.google.android.material.chip.ChipGroup
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.main.*
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton

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

    private val TAG = "FeedDetail"
    private var currentMainIndex: Int = 0

    private val HASHTAG_COLOR = Color.parseColor("#FF0000")


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
    @RequiresApi(Build.VERSION_CODES.O)
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
                    // 코드 집합
                    val moodCodes = setOf("TRADITIONAL","UNIQUE","EMOTIONAL","HEALING","GOODVIEW","ACTIVITY","LOCAL")
                    val foodCodes = setOf("BUNSIK","CAFE_DESSERT","CHICKEN","CHINESE","KOREAN","PORK_SASHIMI","FASTFOOD",
                        "JOKBAL_BOSSAM","PIZZA","WESTERN","MEAT","ASIAN","DOSIRAK","LATE_NIGHT","JJIM_TANG")
                    val compCodes = setOf("ALONE","FRIEND","COUPLE","FAMILY","GROUP","WITH_PET","ALUMNI")

                    // currentCategories(코드)를 -> 라벨(한글)로
                    val preMoods = currentCategories.filter { it in moodCodes }
                        .map { CategoryLabels.toKorean(it) }
                        .distinct()
                    val preFoods = currentCategories.filter { it in foodCodes }
                        .map { CategoryLabels.toKorean(it) }
                        .distinct()
                    val preComps = currentCategories.filter { it in compCodes }
                        .map { CategoryLabels.toKorean(it) }
                        .distinct()
                    val preRegions = ArrayList(currentRegions) // "Province:City" 그대로 (다이얼로그가 내부 변환)

                    CategorySelectDialog.newInstance(
                        ArrayList(preMoods),
                        ArrayList(preFoods),
                        ArrayList(preComps),
                        preRegions
                    ).show(parentFragmentManager, "category_select")
                }
                FeedActionsBottomSheet.ACTION_EDIT_COVER -> {
                    Log.d(TAG, "Open CoverPickSheet: feedId=$feedIdArg, size=${photoUrls.size}, currentMainIndex=$currentMainIndex")
                    if (photoUrls.isEmpty()) {
                        Toast.makeText(requireContext(), "대표사진으로 지정할 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        FeedCoverPickBottomSheet
                            .newInstance(feedIdArg, ArrayList(photoUrls), currentMainIndex)
                            .show(parentFragmentManager, "cover_pick")
                    }
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
                        colorizeHashtagsInto(tvCaption, newContent)
                        Toast.makeText(requireContext(), "글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "수정 실패: ${ok.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            FeedCoverPickBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            val fromSheetFeedId = b.getLong(FeedCoverPickBottomSheet.KEY_FEED_ID, 0L)
            if (fromSheetFeedId != feedId) return@setFragmentResultListener

            val newIndex = b.getInt(FeedCoverPickBottomSheet.KEY_NEW_INDEX, -1)
            val oldIndex = b.getInt(FeedCoverPickBottomSheet.KEY_OLD_INDEX, currentMainIndex)

            Log.d(TAG, "CoverPick result: old=$oldIndex → new=$newIndex")

            if (newIndex < 0 || newIndex >= photoUrls.size) return@setFragmentResultListener
            if (newIndex == currentMainIndex) {
                Log.d(TAG, "Cover not changed (same index=$newIndex). Skip.")
                return@setFragmentResultListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                sendModifyMainPhoto(newIndex)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            CategorySelectDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, b ->
            val labelsM = b.getStringArrayList(CategorySelectDialog.KEY_MOODS).orEmpty()
            val labelsF = b.getStringArrayList(CategorySelectDialog.KEY_FOODS).orEmpty()
            val labelsC = b.getStringArrayList(CategorySelectDialog.KEY_COMPANIONS).orEmpty()
            val payload = b.getStringArrayList(CategorySelectDialog.KEY_PAYLOAD).orEmpty()

            // payload 예: ["moods:UNIQUE", "foods:KOREAN", "companions:FRIEND", "regions:Seoul:Gangnam-gu", ...]
            val categoriesSrv = payload.filter { it.startsWith("moods:") || it.startsWith("foods:") || it.startsWith("companions:") }
            val regionsSrv = payload.filter { it.startsWith("regions:") }.map { it.removePrefix("regions:") }

            Log.d(TAG, "Category payload received → categories=$categoriesSrv, regions=$regionsSrv")
            Log.d(TAG, "Category labels → moods=$labelsM, foods=$labelsF, comps=$labelsC")

            viewLifecycleOwner.lifecycleScope.launch {
                sendModifyCategoryByPayload(categoriesSrv, regionsSrv, labelsM + labelsF + labelsC)
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
                colorizeHashtagsInto(tvCaption, resp.content)

                imgAvatar.load(resp.profileUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_red) // 기본 이미지 (res/drawable/)
                    error(R.drawable.ic_profile_red) // 실패 시 표시
                    transformations(CircleCropTransformation()) // 동그랗게 자르기
                }

                photoUrls = resp.feedPhotoUrl ?: emptyList()
                currentMainIndex = 0 // 서버가 main seq를 주면 해당 값 - 1로 설정
                Log.d(TAG, "Detail loaded: photos=${photoUrls.size}, currentMainIndex=$currentMainIndex")
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

    private fun colorizeHashtagsInto(tv: TextView, text: String?) {
        val raw = text.orEmpty()
        if (raw.isBlank()) {
            tv.text = raw
            return
        }
        val ssb = SpannableStringBuilder(raw)
        // 공백/해시가 아닌 문자들로 이어진 #토큰을 색칠
        val regex = Regex("""#([^\s#]+)""")
        regex.findAll(raw).forEach { m ->
            ssb.setSpan(
                ForegroundColorSpan(HASHTAG_COLOR),
                m.range.first,
                m.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tv.text = ssb
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
        val ctx = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_delete_confirm, null, false)

        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnDelete = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnDelete.setOnClickListener {
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
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
        val ctx = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_report_confirm, null, false)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnReport = view.findViewById<MaterialButton>(R.id.btnUnfollow).apply {
            text = "신고"
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnReport.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Network.feedApi(requireContext()).reportFeed(feedId)
                    Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "신고 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        dialog.show()
        // 둥근 모서리 보이게 기본 배경 제거 + (원하면) 가로 폭 확장
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private suspend fun sendModifyMainPhoto(newIndex: Int) {
        val sequenceBase = 1 // 서버의 sequence가 1부터 시작한다고 가정
        val req = ModifyMainFeedPhotoRequest(
            feedId = feedId,
            newMainFeedPhotoSequence = newIndex + sequenceBase,
            oldMainFeedPhotoSequence = currentMainIndex + sequenceBase
        )
        Log.d(TAG, "modifyMainPhoto: req=$req")

        try {
            val api = Network.feedApi(requireContext())
            val res = withContext(Dispatchers.IO) { api.modifyMainFeedPhoto(req) }
            Log.d(TAG, "modifyMainPhoto: res=$res")

            if (res.isAvailable) {
                // ✅ UI에서 대표사진을 맨 앞으로 이동 (ViewPager 재바인딩)
                val mutable = photoUrls.toMutableList()
                val moved = mutable.removeAt(newIndex)
                mutable.add(0, moved)
                photoUrls = mutable

                currentMainIndex = 0
                viewPager.adapter = PhotoPagerAdapter(photoUrls)
                viewPager.setCurrentItem(0, false)
                tvPageBadge.text = if (photoUrls.isEmpty()) "0/0" else "1/${photoUrls.size}"

                Toast.makeText(requireContext(), "대표사진이 변경되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "대표사진 변경 실패", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "modifyMainPhoto error", e)
            Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sendModifyCategoryByPayload(
        categoriesSrv: List<String>,     // 이미 "moods:CODE" 등 prefix 포함 상태
        regionsSrv: List<String>,        // "Province:City"
        labelDisplay: List<String>       // 칩 임시 표시용(한글 라벨)
    ) {
        Log.d(TAG, "modifyCategory(req) feedId=$feedId, categoriesSrv=$categoriesSrv, regionsSrv=$regionsSrv")
        try {
            val api = Network.feedApi(requireContext())
            val res = withContext(Dispatchers.IO) {
                api.modifyFeedCategory(
                    ModifyFeedCategoryRequest(
                        feedId = feedId,
                        categories = categoriesSrv,
                        regions = regionsSrv
                    )
                )
            }
            Log.d(TAG, "modifyCategory(res): code=${res.code()} isSuccessful=${res.isSuccessful}")

            if (res.isSuccessful) {
                Toast.makeText(requireContext(), "카테고리가 변경되었습니다.", Toast.LENGTH_SHORT).show()

                // 1) 빠른 반영(임시): 라벨 + 지역토큰으로 칩 표시
                //    지역은 "Province:City"를 그대로 넘기면 toDisplayLabel에서 한글로 바꿔줍니다.
                currentCategories = labelDisplay
                currentRegions = ArrayList(regionsSrv)
                renderCategoryChips(currentCategories + currentRegions)

                // 2) 정합성 보장: 서버 기준으로 다시 로드(토큰/순서/정열 포함)
                loadFeedDetail(feedId)
            } else {
                Toast.makeText(requireContext(), "카테고리 변경 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "modifyCategory error", e)
            Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


}
private val moodMapLocal = mapOf(
    "전통적인" to "TRADITIONAL",
    "이색적인" to "UNIQUE",
    "감성적인" to "EMOTIONAL",
    "힐링되는" to "HEALING",
    "뷰 맛집" to "GOODVIEW",
    "활기찬" to "ACTIVITY",
    "로컬" to "LOCAL"
)
private val foodMapLocal = mapOf(
    "분식" to "BUNSIK",
    "카페/디저트" to "CAFE_DESSERT",
    "치킨" to "CHICKEN",
    "중식" to "CHINESE",
    "한식" to "KOREAN",
    "돈까스/회" to "PORK_SASHIMI",
    "패스트푸드" to "FASTFOOD",
    "족발/보쌈" to "JOKBAL_BOSSAM",
    "피자" to "PIZZA",
    "양식" to "WESTERN",
    "고기" to "MEAT",
    "아시안" to "ASIAN",
    "도시락" to "DOSIRAK",
    "야식" to "LATE_NIGHT",
    "찜/탕" to "JJIM_TANG"
)
private val companionMapLocal = mapOf(
    "혼밥" to "ALONE",
    "친구" to "FRIEND",
    "연인" to "COUPLE",
    "가족" to "FAMILY",
    "단체" to "GROUP",
    "반려동물 동반" to "WITH_PET",
    "동호회" to "ALUMNI"
)
// 지역 맵 (필요한 부분만 발췌해도 됨)
private val provinceMapLocal = mapOf(
    "서울" to "Seoul",
    "경기" to "Gyeonggi",
    "인천" to "Incheon",
    "강원" to "Gangwon",
    "대전" to "Daejeon",
    "세종" to "Sejong",
    "충남" to "Chungnam",
    "충북" to "Chungbuk",
    "부산" to "Busan",
    "울산" to "Ulsan",
    "경남" to "Gyeongnam",
    "경북" to "Gyeongbuk",
    "대구" to "Daegu",
    "광주" to "Gwangju",
    "전남" to "Jeonnam",
    "전북" to "Jeonbuk",
    "제주" to "Jeju"
)
private val cityMapLocal = mapOf(
    "강남구" to "Gangnam-gu",
    "강동구" to "Gangdong-gu",
    "강북구" to "Gangbuk-gu",
    "강서구" to "Gangseo-gu",
    "관악구" to "Gwanak-gu",
    "광진구" to "Gwangjin-gu",
    "구로구" to "Guro-gu",
    "금천구" to "Geumcheon-gu",
    "노원구" to "Nowon-gu",
    "도봉구" to "Dobong-gu",
    "동대문구" to "Dongdaemun-gu",
    "동작구" to "Dongjak-gu",
    "마포구" to "Mapo-gu",
    "서대문구" to "Seodaemun-gu",
    "서초구" to "Seocho-gu",
    "성동구" to "Seongdong-gu",
    "성북구" to "Seongbuk-gu",
    "송파구" to "Songpa-gu",
    "양천구" to "Yangcheon-gu",
    "영등포구" to "Yeongdeungpo-gu",
    "용산구" to "Yongsan-gu",
    "은평구" to "Eunpyeong-gu",
    "종로구" to "Jongno-gu",
    "중구" to "Jung-gu",
    "중랑구" to "Jungnang-gu",

    // --- Gyeonggi
    "가평군" to "Gapyeong-gun",
    "고양시" to "Goyang-si",
    "과천시" to "Gwacheon-si",
    "광명시" to "Gwangmyeong-si",
    "광주시" to "Gwangju-si",
    "구리시" to "Guri-si",
    "군포시" to "Gunpo-si",
    "김포시" to "Gimpo-si",
    "남양주시" to "Namyangju-si",
    "동두천시" to "Dongducheon-si",
    "부천시" to "Bucheon-si",
    "성남시" to "Seongnam-si",
    "수원시" to "Suwon-si",
    "시흥시" to "Siheung-si",
    "안산시" to "Ansan-si",
    "안성시" to "Anseong-si",
    "안양시" to "Anyang-si",
    "양주시" to "Yangju-si",
    "양평군" to "Yangpyeong-gun",
    "여주시" to "Yeoju-si",
    "연천군" to "Yeoncheon-gun",
    "오산시" to "Osan-si",
    "용인시" to "Yongin-si",
    "의왕시" to "Uiwang-si",
    "의정부시" to "Uijeongbu-si",
    "이천시" to "Icheon-si",
    "파주시" to "Paju-si",
    "포천시" to "Pocheon-si",
    "평택시" to "Pyeongtaek-si",
    "하남시" to "Hanam-si",
    "화성시" to "Hwaseong-si",

    // --- Incheon
    "강화군" to "Ganghwa-gun",
    "계양구" to "Gyeyang-gu",
    "남동구" to "Namdong-gu",
    "동구" to "Dong-gu",
    "미추홀구" to "Michuhol-gu",
    "부평구" to "Bupyeong-gu",
    "서구" to "Seo-gu",
    "연수구" to "Yeonsu-gu",
    "옹진군" to "Ongjin-gun",
    "중구(인천)" to "Jung-gu",   // 중복되니 구분 필요하면 키 바꿔

    // --- Gangwon
    "강릉시" to "Gangneung-si",
    "고성군(강원)" to "Goseong-gun",
    "동해시" to "Donghae-si",
    "삼척시" to "Samcheok-si",
    "속초시" to "Sokcho-si",
    "양구군" to "Yanggu-gun",
    "양양군" to "Yangyang-gun",
    "영월군" to "Yeongwol-gun",
    "원주시" to "Wonju-si",
    "인제군" to "Inje-gun",
    "정선군" to "Jeongseon-gun",
    "춘천시" to "Chuncheon-si",
    "철원군" to "Cheorwon-gun",
    "태백시" to "Taebaek-si",
    "평창군" to "Pyeongchang-gun",
    "횡성군" to "Hoengseong-gun",
    "홍천군" to "Hongcheon-gun",
    "화천군" to "Hwacheon-gun",

    // --- Daejeon
    "대덕구" to "Daedeok-gu",
    "동구(대전)" to "Dong-gu",
    "서구(대전)" to "Seo-gu",
    "유성구" to "Yuseong-gu",
    "중구(대전)" to "Jung-gu",

    // --- Sejong
    "세종시" to "Sejong-si",

    // --- Chungnam
    "계룡시" to "Gyeryong-si",
    "공주시" to "Gongju-si",
    "금산군" to "Geumsan-gun",
    "논산시" to "Nonsan-si",
    "당진시" to "Dangjin-si",
    "보령시" to "Boryeong-si",
    "부여군" to "Buyeo-gun",
    "서산시" to "Seosan-si",
    "서천군" to "Seocheon-gun",
    "아산시" to "Asan-si",
    "예산군" to "Yesan-gun",
    "천안시" to "Cheonan-si",
    "청양군" to "Cheongyang-gun",
    "태안군" to "Taean-gun",
    "홍성군" to "Hongseong-gun",

    // --- Chungbuk
    "괴산군" to "Goesan-gun",
    "단양군" to "Danyang-gun",
    "보은군" to "Boeun-gun",
    "영동군" to "Yeongdong-gun",
    "옥천군" to "Okcheon-gun",
    "음성군" to "Eumseong-gun",
    "증평군" to "Jeungpyeong-gun",
    "진천군" to "Jincheon-gun",
    "제천시" to "Jecheon-si",
    "청주시" to "Cheongju-si",
    "충주시" to "Chungju-si",

    // --- Busan
    "강서구(부산)" to "Gangseo-gu",
    "금정구" to "Geumjeong-gu",
    "기장군" to "Gijang-gun",
    "남구(부산)" to "Nam-gu",
    "동구(부산)" to "Dong-gu",
    "동래구" to "Dongnae-gu",
    "부산진구" to "Busanjin-gu",
    "북구(부산)" to "Buk-gu",
    "사상구" to "Sasang-gu",
    "사하구" to "Saha-gu",
    "서구(부산)" to "Seo-gu",
    "수영구" to "Suyeong-gu",
    "영도구" to "Yeongdo-gu",
    "연제구" to "Yeonje-gu",
    "중구(부산)" to "Jung-gu",
    "해운대구" to "Haeundae-gu",

    // --- Ulsan
    "남구(울산)" to "Nam-gu",
    "동구(울산)" to "Dong-gu",
    "북구(울산)" to "Buk-gu",
    "울주군" to "Ulju-gun",
    "중구(울산)" to "Jung-gu",

    // --- Gyeongnam
    "거제시" to "Geoje-si",
    "거창군" to "Geochang-gun",
    "고성군(경남)" to "Goseong-gun",
    "김해시" to "Gimhae-si",
    "남해군" to "Namhae-gun",
    "밀양시" to "Miryang-si",
    "사천시" to "Sacheon-si",
    "산청군" to "Sancheong-gun",
    "양산시" to "Yangsan-si",
    "의령군" to "Uiryeong-gun",
    "진주시" to "Jinju-si",
    "창녕군" to "Changnyeong-gun",
    "창원시" to "Changwon-si",
    "통영시" to "Tongyeong-si",
    "하동군" to "Hadong-gun",
    "함안군" to "Haman-gun",
    "함양군" to "Hamyang-gun",
    "합천군" to "Hapcheon-gun",

    // --- Gyeongbuk
    "경산시" to "Gyeongsan-si",
    "경주시" to "Gyeongju-si",
    "고령군" to "Goryeong-gun",
    "구미시" to "Gumi-si",
    "김천시" to "Gimcheon-si",
    "문경시" to "Mungyeong-si",
    "봉화군" to "Bonghwa-gun",
    "상주시" to "Sangju-si",
    "성주군" to "Seongju-gun",
    "안동시" to "Andong-si",
    "영덕군" to "Yeongdeok-gun",
    "영양군" to "Yeongyang-gun",
    "영주시" to "Yeongju-si",
    "영천시" to "Yeongcheon-si",
    "예천군" to "Yecheon-gun",
    "울릉군" to "Ulleung-gun",
    "울진군" to "Uljin-gun",
    "의성군" to "Uiseong-gun",
    "청도군" to "Cheongdo-gun",
    "청송군" to "Cheongsong-gun",
    "칠곡군" to "Chilgok-gun",
    "포항시" to "Pohang-si",

    // --- Daegu
    "군위군" to "Gunwi-gun",
    "남구(대구)" to "Nam-gu",
    "달서구" to "Dalseo-gu",
    "달성군" to "Dalseong-gun",
    "동구(대구)" to "Dong-gu",
    "북구(대구)" to "Buk-gu",
    "서구(대구)" to "Seo-gu",
    "수성구" to "Suseong-gu",
    "중구(대구)" to "Jung-gu",

    // --- Gwangju
    "광산구" to "Gwangsan-gu",
    "남구(광주)" to "Nam-gu",
    "동구(광주)" to "Dong-gu",
    "북구(광주)" to "Buk-gu",
    "서구(광주)" to "Seo-gu",

    // --- Jeonnam
    "강진군" to "Gangjin-gun",
    "고흥군" to "Goheung-gun",
    "곡성군" to "Gokseong-gun",
    "광양시" to "Gwangyang-si",
    "구례군" to "Gurye-gun",
    "나주시" to "Naju-si",
    "담양군" to "Damyang-gun",
    "목포시" to "Mokpo-si",
    "무안군" to "Muan-gun",
    "보성군" to "Boseong-gun",
    "순천시" to "Suncheon-si",
    "신안군" to "Shinan-gun",
    "여수시" to "Yeosu-si",
    "영광군" to "Yeonggwang-gun",
    "영암군" to "Yeongam-gun",
    "완도군" to "Wando-gun",
    "장성군" to "Jangseong-gun",
    "장흥군" to "Jangheung-gun",
    "진도군" to "Jindo-gun",
    "함평군" to "Hampyeong-gun",
    "해남군" to "Haenam-gun",
    "화순군" to "Hwasun-gun",

    // --- Jeonbuk
    "고창군" to "Gochang-gun",
    "군산시" to "Gunsan-si",
    "김제시" to "Gimje-si",
    "남원시" to "Namwon-si",
    "무주군" to "Muju-gun",
    "부안군" to "Buan-gun",
    "순창군" to "Sunchang-gun",
    "완주군" to "Wanju-gun",
    "익산시" to "Iksan-si",
    "임실군" to "Imsil-gun",
    "장수군" to "Jangsu-gun",
    "전주시" to "Jeonju-si",
    "정읍시" to "Jeongeup-si",
    "진안군" to "Jinan-gun",

    // --- Jeju
    "서귀포시" to "Seogwipo-si",
    "제주시" to "Jeju-si"
)