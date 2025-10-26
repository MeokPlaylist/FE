package com.meokpli.app.user

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.meokpli.app.R
import com.meokpli.app.auth.AuthApi
import com.meokpli.app.auth.Network
import com.meokpli.app.main.Feed.PresignedUploader
import kotlinx.coroutines.*
import java.time.LocalDateTime
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.exifinterface.media.ExifInterface

class InitProfileActivity : AppCompatActivity() {

    private lateinit var imageProfile: ImageView
    private lateinit var btnClearPhoto: ImageButton
    private lateinit var editNickname: EditText
    private lateinit var textNicknameCount: TextView
    private lateinit var textIntroCount: TextView
    private lateinit var editIntro: EditText
    private lateinit var buttonNext: Button
    private lateinit var tvNicknameError: TextView

    private val NICKNAME_LIMIT = 10
    private val INTRO_LIMIT = 30
    private var selectedImageUri: Uri? = null
    private lateinit var userApi: UserApi
    private lateinit var authApi: AuthApi

    private var checkJob: Job? = null
    private var lastCheckedNickname: String = ""
    private var lastIsAvailable: Boolean? = null

    // 갤러리에서 이미지 선택 런처
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageProfile.setImageURI(it)
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
        setContentView(R.layout.activity_init_profile)

        imageProfile = findViewById(R.id.imageProfile)
        btnClearPhoto = findViewById(R.id.btnClearPhoto)
        editNickname = findViewById(R.id.editNickname)
        textNicknameCount = findViewById(R.id.textNicknameCount)
        textIntroCount = findViewById(R.id.textIntroCount)
        editIntro = findViewById(R.id.editIntro)
        buttonNext = findViewById(R.id.buttonNext)
        tvNicknameError = findViewById(R.id.tvNicknameError)

        userApi = Network.userApi(this)
        authApi = Network.authApi(this)

        // 기본은 숨김
        btnClearPhoto.visibility = View.GONE

        // 뒤로가기 시 동의 화면으로 복귀
        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@InitProfileActivity, ConsentFormActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // 글자수 제한
        editNickname.filters = arrayOf(InputFilter.LengthFilter(NICKNAME_LIMIT))
        editIntro.filters = arrayOf(InputFilter.LengthFilter(INTRO_LIMIT))

        // 닉네임 입력 감시
        editNickname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                textNicknameCount.text = "$count/$NICKNAME_LIMIT"

                val nickname = s?.toString()?.trim().orEmpty()
                if (nickname.isEmpty()) {
                    tvNicknameError.text = ""
                    tvNicknameError.visibility = View.GONE
                    lastCheckedNickname = ""
                    lastIsAvailable = null
                    return
                }

                checkJob?.cancel()
                checkJob = lifecycleScope.launch {
                    delay(400)
                    performDuplicateCheck(nickname)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 소개글 글자수 업데이트
        editIntro.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                textIntroCount.text = "$count/$INTRO_LIMIT"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 사진 선택
        val openPicker = { pickImage.launch("image/*") }
        imageProfile.setOnClickListener { openPicker() }

        // X 버튼 클릭 → 기본 이미지로 복귀
        btnClearPhoto.setOnClickListener {
            selectedImageUri = null
            imageProfile.setImageResource(R.drawable.ic_profile_red)
            btnClearPhoto.visibility = View.GONE
        }

        // 다음 버튼 클릭 → 프로필/닉네임 저장
        buttonNext.setOnClickListener {
            val nickname = editNickname.text.toString().trim()
            val intro = editIntro.text.toString().trim()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (selectedImageUri != null) {
                        val fileName = "profile_${System.currentTimeMillis()}.jpg"
                        val setupResp = userApi.savePhoto(
                            UserProfileSetupRequest(fileName, LocalDateTime.now().toString())
                        )

                        val uploaded = PresignedUploader.uploadAll(
                            context = this@InitProfileActivity,
                            uris = listOf(selectedImageUri!!),
                            urls = listOf(setupResp.profilePutPresignedUrl)
                        )

                        if (!uploaded.all { it }) throw Exception("프로필 업로드 실패")
                    }

                    userApi.saveDetail(UserDetailRequest(nickname, intro))

                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@InitProfileActivity, CategoryActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("프로필 저장 실패: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun performDuplicateCheck(nickname: String) {
        if (lastCheckedNickname == nickname && lastIsAvailable != null) {
            withContext(Dispatchers.Main) {
                if (lastIsAvailable == false) {
                    tvNicknameError.text = "이미 사용 중인 닉네임입니다."
                    tvNicknameError.visibility = View.VISIBLE
                } else {
                    tvNicknameError.text = ""
                    tvNicknameError.visibility = View.GONE
                }
            }
            return
        }

        withContext(Dispatchers.IO) {
            runCatching {
                authApi.checkNickname(nickname)
            }.onSuccess { resp ->
                lastCheckedNickname = nickname
                lastIsAvailable = resp.isAvailable
                withContext(Dispatchers.Main) {
                    if (!resp.isAvailable) {
                        tvNicknameError.text = "이미 사용 중인 닉네임입니다."
                        tvNicknameError.visibility = View.VISIBLE
                    } else {
                        tvNicknameError.text = ""
                        tvNicknameError.visibility = View.GONE
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    tvNicknameError.text = "중복 검사 실패: ${e.message}"
                    tvNicknameError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus!!.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
