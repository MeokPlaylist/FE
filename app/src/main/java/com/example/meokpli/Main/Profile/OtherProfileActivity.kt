package com.example.meokpli.Main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.Interaction.FollowListFragment
import com.example.meokpli.Main.Interaction.OtherFollowListFragment
import com.example.meokpli.R
import com.example.meokpli.Main.SocialInteractionApi
import com.example.meokpli.User.UserApi
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OtherProfileActivity : AppCompatActivity() {

    private val userApi: UserApi by lazy { Network.userApi(this) }
    private val socialApi: SocialInteractionApi by lazy { Network.socialApi(this) }

    private lateinit var nickname: String
    private var isMe = false
    private var isFollowing = false
    private var followsMe = false
    private var followersCount = 0

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
        // 프래그먼트 레이아웃 재사용
        setContentView(R.layout.fragment_profile)

        nickname = intent.getStringExtra(EXTRA_NICKNAME) ?: ""
        bindViews()
        bindClicks()
        loadProfile()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        ivAvatar = findViewById(R.id.imageAvatar)
        tvTitle = findViewById(R.id.textTitle)         // ← 추가한 id 사용
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

        // 게시물 클릭 → 게시물 리스트 이동
        tvPost.setOnClickListener {
            // TODO: PostListActivity.start(this, nickname)
        }

        // 팔로잉 리스트 이동
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
                // 서버: 닉네임으로 사용자 페이지 + isMe 반환
                val res = withContext(Dispatchers.IO) { userApi.getUserPage(nickname) }

                // 공통 바인딩
                tvNickname.text = res.userNickname
                tvIntro.text = res.userIntro.orEmpty()
                tvPost.text = res.feedNum.toString()
                tvFollowing.text = res.followingNum.toString()
                tvFollowers.text = res.followerNum.toString()
                followersCount = res.followerNum.toInt()

                // 아바타
                if (!res.profileUrl.isNullOrBlank()) {
                    ivAvatar.load(res.profileUrl) {
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                        crossfade(true)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile_red)
                }

                // 분기
                isMe = res.isMe
                if (isMe) {
                    // 내 계정 UI
                    tvTitle.text = "내 계정"
                    tvSettings.visibility = View.VISIBLE
                    viewSettingsLine.visibility = View.VISIBLE
                    btnFollow.visibility = View.GONE
                } else {
                    // 남의 계정 UI
                    tvTitle.text = "${res.userNickname}의 계정"
                    tvSettings.visibility = View.GONE
                    viewSettingsLine.visibility = View.GONE

                    // 초기 팔로우 상태 (백엔드 확장 전까진 기본값)
                    isFollowing = false
                    followsMe = false
                    btnFollow.visibility = View.VISIBLE
                    renderFollowUi()
                }

            } catch (e: HttpException) {
                val msg = if (e.code() == 401) "로그인이 필요합니다." else "프로필 로딩 실패 (${e.code()})"
                Toast.makeText(this@OtherProfileActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@OtherProfileActivity, "프로필을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
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
                    withContext(Dispatchers.IO) { Network.socialApi(this@OtherProfileActivity).unFollow(nickname) }
                    isFollowing = false
                    followersCount = (followersCount - 1).coerceAtLeast(0)
                } else {
                    withContext(Dispatchers.IO) { Network.socialApi(this@OtherProfileActivity).follow(nickname) }
                    isFollowing = true
                    followersCount += 1
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
