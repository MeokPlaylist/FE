package com.meokpli.app.user

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.main.Feed.PresignedUploader
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnClearPhoto: ImageButton
    private lateinit var tvChangePhoto: TextView
    private lateinit var etNickname: EditText
    private lateinit var etBio: EditText
    private lateinit var tvNickCount: TextView
    private lateinit var tvBioCount: TextView
    private lateinit var btnSubmit: Button
    private lateinit var api: UserApi

    private var selectedImageUri: Uri? = null
    private var currentProfileUrl: String? = null // 서버에서 받은 기존 사진 URL

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivAvatar.setImageURI(it)
            btnClearPhoto.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        //상태바 표시
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_edit_profile)

        api = Network.userApi(this)

        ivAvatar = findViewById(R.id.ivAvatar)
        btnClearPhoto = findViewById(R.id.btnClearPhoto)
        tvChangePhoto = findViewById(R.id.tvChangePhoto)
        etNickname = findViewById(R.id.etNickname)
        etBio = findViewById(R.id.etBio)
        tvNickCount = findViewById(R.id.tvNickCount)
        tvBioCount = findViewById(R.id.tvBioCount)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnClearPhoto.visibility = View.GONE

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 사진 선택 열기
        val openPicker = { pickImage.launch("image/*") }
        ivAvatar.setOnClickListener { openPicker() }
        tvChangePhoto.setOnClickListener { openPicker() }

        // X 버튼 클릭 시 초기화
        btnClearPhoto.setOnClickListener {
            selectedImageUri = null
            currentProfileUrl = null
            ivAvatar.setImageResource(R.drawable.ic_profile_red)
            btnClearPhoto.visibility = View.GONE
        }

        // 글자수 카운트
        etNickname.addTextChangedListener(counterWatcher { c -> tvNickCount.text = "$c / 10" })
        etBio.addTextChangedListener(counterWatcher { c -> tvBioCount.text = "$c / 20" })

        // 기존 프로필 불러오기
        loadMyProfile()

        btnSubmit.setOnClickListener { submit() }
    }

    private fun counterWatcher(onChanged: (Int) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.length ?: 0)
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun validate(): String? {
        val nick = etNickname.text?.toString()?.trim().orEmpty()
        val bio = etBio.text?.toString()?.trim().orEmpty()
        if (nick.isEmpty()) return "닉네임을 입력해 주세요."
        if (nick.length !in 1..10) return "닉네임은 1~10글자여야 합니다."
        if (bio.isEmpty()) return "소개를 입력해 주세요."
        if (bio.length > 20) return "소개는 20자 이하여야 합니다."
        return null
    }

    private fun loadMyProfile() {
        lifecycleScope.launch {
            try {
                val res = api.getMyProfile()
                etNickname.setText(res.userNickname)
                etBio.setText(res.userIntro)
                tvNickCount.text = "${res.userNickname.length} / 10"
                tvBioCount.text = "${res.userIntro.length} / 20"

                currentProfileUrl = res.profileUrl
                if (!currentProfileUrl.isNullOrBlank()) {
                    ivAvatar.load(currentProfileUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile_red)
                        error(R.drawable.ic_profile_red)
                    }
                    btnClearPhoto.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditProfileActivity, "프로필 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submit() {
        val error = validate()
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return
        }

        val nick = etNickname.text.toString().trim()
        val bio = etBio.text.toString().trim()

        btnSubmit.isEnabled = false
        btnSubmit.text = "저장 중..."

        lifecycleScope.launch {
            try {
                // 새 이미지가 선택된 경우만 업로드
                if (selectedImageUri != null) {
                    val fileName = "profile_${System.currentTimeMillis()}.jpg"
                    val setupResp = api.savePhoto(
                        UserProfileSetupRequest(fileName, LocalDateTime.now().toString())
                    )

                    val uploaded = PresignedUploader.uploadAll(
                        this@EditProfileActivity,
                        listOf(selectedImageUri!!),
                        listOf(setupResp.profilePutPresignedUrl)
                    )

                    if (!uploaded.all { it }) throw Exception("프로필 사진 업로드 실패")
                }

                api.saveDetail(UserDetailRequest(nickname = nick, introduction = bio))
                Toast.makeText(this@EditProfileActivity, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditProfileActivity, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = "변경완료"
            }
        }
    }
}
