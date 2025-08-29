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
import com.example.meokpli.Auth.Network
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private val userApi by lazy { Network.userApi(this) }
    private val socialApi by lazy { Network.socialApi(this) }

    private lateinit var nickname: String
    private var isMe = false
    private var isFollowing = false
    private var followsMe = false // 서버에서 내려주면 사용
    private var followersCount = 0

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var ivAvatar: ImageView
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
        setContentView(R.layout.fragment_profile)  // XML 재사용 ✅

        nickname = intent.getStringExtra(EXTRA_NICKNAME) ?: ""
        bindViews()
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadProfile()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        ivAvatar = findViewById(R.id.imageAvatar)
        tvNickname = findViewById(R.id.textNickname)
        tvIntro = findViewById(R.id.textBio)
        tvPost = findViewById(R.id.textPostCount)
        tvFollowing = findViewById(R.id.textFollowing)
        tvFollowers = findViewById(R.id.textFollowers)
        tvSettings = findViewById(R.id.textSettings)
        viewSettingsLine = findViewById(R.id.viewSettingsLine)
        btnFollow = findViewById(R.id.btnFollow)
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val me = userApi.getMyPage() // 내 프로필 조회
                isMe = (nickname.isBlank() || nickname == me.userNickname)

                if (isMe) {
                    bindProfile(
                        me.userNickname,
                        me.userIntro,
                        me.feedNum.toInt(),
                        me.followingNum.toInt(),
                        me.followerNum.toInt(),
                        me.profileUrl
                    )
                    showSelfHeader()
                } else {
                    // ★ 닉네임 기반 다른 사용자 프로필 조회 API 필요 시 여기에 추가
                    showOtherHeader()
                    renderFollowUi()
                    btnFollow.setOnClickListener { toggleFollow() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@UserProfileActivity, "프로필을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindProfile(
        nickname: String,
        intro: String?,
        post: Int,
        following: Int,
        followers: Int,
        avatarUrl: String?
    ) {
        tvNickname.text = nickname
        tvIntro.text = intro ?: ""
        tvPost.text = post.toString()
        tvFollowing.text = following.toString()
        tvFollowers.text = followers.toString()
        followersCount = followers

        ivAvatar.setImageResource(R.drawable.ic_profile_red) // TODO: Coil 등 적용 가능
    }

    private fun showSelfHeader() {
        tvSettings.visibility = View.VISIBLE
        viewSettingsLine.visibility = View.VISIBLE
        btnFollow.visibility = View.GONE
    }

    private fun showOtherHeader() {
        tvSettings.visibility = View.GONE
        viewSettingsLine.visibility = View.GONE
        btnFollow.visibility = View.VISIBLE
    }

    /** 팔로우 상태에 따른 MaterialButton 스타일링 */
    private fun renderFollowUi() {
        when {//내가 팔로우하고 상대도 팔로우를 했을 경우
            isFollowing && followsMe -> {
                btnFollow.text = "팔로잉"
                btnFollow.background = ContextCompat.getDrawable(this, R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }//내가 팔로우 했을경우
            isFollowing -> {
                btnFollow.text = "팔로잉"
                btnFollow.background = ContextCompat.getDrawable(this, R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }//날 팔로우 했을 경우
            followsMe -> {
                btnFollow.text = "맞팔로우"
                btnFollow.background = ContextCompat.getDrawable(this, R.drawable.btn_mutual_follow)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }//이것도아니고 저것도 아님 머겠어?
            else -> {
                btnFollow.text = "팔로우"
                btnFollow.background = ContextCompat.getDrawable(this, R.drawable.btn_basic)
                btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }
        btnFollow.isEnabled = true
    }

    private fun toggleFollow() {
        btnFollow.isEnabled = false
        lifecycleScope.launch {
            try {
                if (isFollowing) {
                    socialApi.unFollow(nickname)
                    isFollowing = false
                    followersCount = (followersCount - 1).coerceAtLeast(0)
                } else {
                    socialApi.follow(nickname)
                    isFollowing = true
                    followersCount++
                }
                tvFollowers.text = followersCount.toString()
                renderFollowUi()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@UserProfileActivity, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                btnFollow.isEnabled = true
            }
        }
    }

    companion object {
        private const val EXTRA_NICKNAME = "extra_nickname"
        fun start(context: Context, nickname: String) {
            context.startActivity(
                Intent(context, UserProfileActivity::class.java)
                    .putExtra(EXTRA_NICKNAME, nickname)
            )
        }
    }
}
