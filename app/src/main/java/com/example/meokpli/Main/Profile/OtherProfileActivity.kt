package com.example.meokpli.Main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.Interaction.OtherFollowListFragment
import com.example.meokpli.Main.Interaction.FollowApi   // ✅ FollowApi 패키지에 주의
import com.example.meokpli.R
import com.example.meokpli.User.UserApi
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OtherProfileActivity : AppCompatActivity() {
     private val socialApi: SocialInteractionApi by lazy { Network.socialApi(this) }
    private val followApi: FollowApi by lazy { Network.followApi(this) }           // ✅ C안용

    private lateinit var nickname: String
    private var isMe = false
    private var isFollowing = false   // 내가 그를 팔로우 중?
    private var followsMe = false     // 그가 나를 팔로우 중?
    private var followersCount = 0L   // Long 일관

    // Views (fragment_profile.xml 재사용)
    private lateinit var btnBack: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvIntro: TextView
    private lateinit var tvPost: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var tvFollowers: TextView
    private lateinit var tvSettings: TextView
    private lateinit var viewSettingsLine: View
    private lateinit var btnFollow: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_profile)

        nickname = intent.getStringExtra(EXTRA_NICKNAME).orEmpty()
        bindViews()
        bindClicks()
        loadProfile()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        ivAvatar = findViewById(R.id.imageAvatar)
        tvTitle = findViewById(R.id.textTitle)
        tvNickname = findViewById(R.id.textNickname)
        tvIntro = findViewById(R.id.textBio)
        tvPost = findViewById(R.id.textPostCount)
        tvFollowing = findViewById(R.id.textFollowing)
        tvFollowers = findViewById(R.id.textFollowers)
        tvSettings = findViewById(R.id.textSettings)
        viewSettingsLine = findViewById(R.id.viewSettingsLine)
        btnFollow = findViewById(R.id.btnFollow)
    }

    private fun bindClicks() {
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvPost.setOnClickListener {
            // TODO: PostListActivity.start(this, nickname)
        }

        tvFollowing.setOnClickListener {
            OtherFollowListFragment.start(this, nickname, "following")
        }
        tvFollowers.setOnClickListener {
            OtherFollowListFragment.start(this, nickname, "followers")
        }

        btnFollow.setOnClickListener { toggleFollow() }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                // 1) 타인 프로필 조회
                val res = withContext(Dispatchers.IO) { socialApi.getUserPage(nickname) }
                Log.d("result!!",res.toString())
                // 2) 바인딩 (널 안전)
                tvNickname.text = res.userNickname ?: nickname
                tvIntro.text = res.userIntro.orEmpty()
                tvPost.text = (res.feedNum ?: 0L).toString()
                tvFollowing.text = (res.followingNum ?: 0L).toString()
                tvFollowers.text = (res.followerNum ?: 0L).toString()
                followersCount = res.followerNum ?: 0L

                val avatarUrl = res.profileUrl
                if (!avatarUrl.isNullOrBlank()) {
                    ivAvatar.load(avatarUrl) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile_red)
                }

                // 3) 내 계정 여부
                isMe = res.isMe == true
                if (isMe) {
                    tvTitle.text = "내 계정"
                    tvSettings.visibility = View.VISIBLE
                    viewSettingsLine.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                    return@launch
                }

                // 4) 타인 계정 UI
                tvTitle.text = "${res.userNickname ?: nickname}의 계정"
                tvSettings.visibility = View.GONE
                viewSettingsLine.visibility = View.GONE
                btnFollow.visibility = View.VISIBLE

                // 5) ✅ 관계 C안: 내 팔로잉/팔로워 목록 일부 페이지를 훑어 상태 추정
                val (f1, f2) = resolveRelationshipSlow(nickname, maxPages = 2, pageSize = 10)
                isFollowing = f1
                followsMe = f2
                renderFollowUi()

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                android.util.Log.e("OtherProfile", "HTTP ${e.code()} body=$body", e)
                val msg = if (e.code() == 401) "로그인이 필요합니다." else "프로필 로딩 실패 (${e.code()})"
                Toast.makeText(this@OtherProfileActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("OtherProfile", "Fail getUserPage", e)
                Toast.makeText(this@OtherProfileActivity, "프로필을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** C안: 내 목록을 최대 N페이지까지 훑어 관계 추정 */
    private suspend fun resolveRelationshipSlow(
        otherNickname: String,
        maxPages: Int = 2,
        pageSize: Int = 10
    ): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        val followingJob = async {
            var page = 0
            while (page < maxPages) {
                val resp = followApi.getFollowingList(page = page, size = pageSize)
                if (resp.content.any { it.nickname == otherNickname }) return@async true
                if (resp.last || resp.content.isEmpty()) break
                page++
            }
            false
        }
        val followersJob = async {
            var page = 0
            while (page < maxPages) {
                val resp = followApi.getFollowerList(page = page, size = pageSize)
                if (resp.content.any { it.nickname == otherNickname }) return@async true
                if (resp.last || resp.content.isEmpty()) break
                page++
            }
            false
        }
        followingJob.await() to followersJob.await()
    }

    /** 팔로우 상태에 따른 버튼 스타일링 */
    private fun renderFollowUi() {
        when {
            isFollowing && followsMe -> {
                btnFollow.text = "맞팔로잉"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
            isFollowing -> {
                btnFollow.text = "팔로잉"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
            followsMe -> {
                btnFollow.text = "맞팔로우"
                btnFollow.setBackgroundResource(R.drawable.btn_mutual_follow)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
            else -> {
                btnFollow.text = "팔로우"
                btnFollow.setBackgroundResource(R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }
        btnFollow.isEnabled = true
    }

    private fun toggleFollow() {
        if (isMe) return
        btnFollow.isEnabled = false

        val prevFollowing = isFollowing
        val prevFollowersCount = followersCount

        lifecycleScope.launch {
            try {
                if (isFollowing) {
                    withContext(Dispatchers.IO) { socialApi.unFollow(nickname) }
                    isFollowing = false
                    followersCount = (followersCount - 1L).coerceAtLeast(0L)  // Long 보정
                } else {
                    withContext(Dispatchers.IO) { socialApi.follow(nickname) }
                    isFollowing = true
                    followersCount = followersCount + 1L                    // Long 보정
                }
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
            } catch (e: Exception) {
                // 실패 시 원복
                isFollowing = prevFollowing
                followersCount = prevFollowersCount
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
                Toast.makeText(this@OtherProfileActivity, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                btnFollow.isEnabled = true
            }
        }
    }

    companion object {
        private const val EXTRA_NICKNAME = "extra_nickname"
        fun start(context: Context, nickname: String) {
            context.startActivity(
                Intent(context, OtherProfileActivity::class.java)
                    .putExtra(EXTRA_NICKNAME, nickname)
            )
        }
    }
}
