package com.example.meokpli.User

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.Auth.Network
import com.example.meokpli.R
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class InitProfileActivity : AppCompatActivity() {

    private lateinit var imageProfile: ImageView
    private lateinit var editNickname: EditText
    private lateinit var textNicknameCount: TextView
    private lateinit var textIntroCount: TextView
    private lateinit var editIntro: EditText
    private lateinit var buttonNext: Button
    private lateinit var tvNicknameError: TextView

    private val PICK_IMAGE_REQUEST = 1
    private val NICKNAME_LIMIT = 10
    private val INTRO_LIMIT = 30

    private lateinit var api: UserApi

    // 닉네임 실시간 중복검사 디바운싱용
    private var checkJob: Job? = null
    private var lastCheckedNickname: String = ""
    private var lastIsAvailable: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init_profile)

        imageProfile = findViewById(R.id.imageProfile)
        editNickname = findViewById(R.id.editNickname)
        textNicknameCount = findViewById(R.id.textNicknameCount)
        textIntroCount = findViewById(R.id.textIntroCount)
        editIntro = findViewById(R.id.editIntro)
        buttonNext = findViewById(R.id.buttonNext)
        tvNicknameError = findViewById(R.id.tvNicknameError)

        // /user/ 베이스 API 사용 (AuthInterceptor로 토큰 자동 부착)
        api = Network.userApi(this) // :contentReference[oaicite:2]{index=2}

        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@InitProfileActivity, ConsentFormActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // 글자수 제한 필터 적용 (UX)
        editNickname.filters = arrayOf(InputFilter.LengthFilter(NICKNAME_LIMIT))
        editIntro.filters = arrayOf(InputFilter.LengthFilter(INTRO_LIMIT))

        // 닉네임 글자 수 실시간 표시 + 중복 검사
        editNickname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                textNicknameCount.text = "$count/$NICKNAME_LIMIT"

                val nickname = s?.toString()?.trim().orEmpty()

                // 비어있으면 에러 제거 및 상태 초기화
                if (nickname.isEmpty()) {
                    tvNicknameError.text = ""
                    tvNicknameError.visibility = View.GONE
                    lastCheckedNickname = ""
                    lastIsAvailable = null
                    return
                }

                // 이전 검사 취소 + 간단 디바운스
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

        // 갤러리에서 사진 선택
        imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 다음 버튼 클릭 시: 최종 닉네임 중복 확인 후 저장
        buttonNext.setOnClickListener {
            val nickname = editNickname.text.toString().trim()
            val intro = editIntro.text.toString().trim()

            if (nickname.isEmpty()) {
                showToast("닉네임을 입력해주세요.")
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                // 마지막 실시간 검사 결과 대신, 최종 한 번 더 서버 확인
                val isAvailable = runCatching {
                    api.checkNickname(NicknameCheckRequest(nickname)).isAvailable
                }.getOrElse {
                    withContext(Dispatchers.Main) {
                        tvNicknameError.text = "중복 검사 실패: ${it.message}"
                        tvNicknameError.visibility = View.VISIBLE
                    }
                    return@launch
                }

                if (!isAvailable) {
                    withContext(Dispatchers.Main) {
                        tvNicknameError.text = "이미 사용 중인 닉네임입니다."
                        tvNicknameError.visibility = View.VISIBLE
                    }
                    return@launch
                }

                // 중복 아님 → 프로필 저장
                runCatching {
                    api.saveDetail(UserDetailRequest(nickname, intro))
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@InitProfileActivity, CategoryActivity::class.java))
                        finish()
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        showToast("프로필 저장 실패: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun performDuplicateCheck(nickname: String) {
        // 같은 문자열에 대한 중복 호출 방지(선택)
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
                api.checkNickname(NicknameCheckRequest(nickname))
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

    @Deprecated("Use ActivityResultContracts")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data!!
            try {
                // (선택) 큰 이미지 대응: 필요 시 다운샘플링 로직 적용
                contentResolver.openInputStream(imageUri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    imageProfile.setImageBitmap(bmp)
                }

                val file = createTempFileFromUriSafe(imageUri)
                val part = MultipartBody.Part.createFormData(
                    name = "profileImg", // 서버 필드명과 동일해야 함
                    filename = file.name,
                    body = file.asRequestBody("image/*".toMediaTypeOrNull())
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        api.savePhoto(part)
                    }.onSuccess {
                        withContext(Dispatchers.Main) { showToast("이미지 업로드 성공!") }
                    }.onFailure { e ->
                        withContext(Dispatchers.Main) { showToast("업로드 실패: ${e.message}") }
                    }
                }

            } catch (e: Exception) {
                showToast("이미지를 불러오지 못했습니다.")
            }
        }
    }

    private fun createTempFileFromUriSafe(uri: Uri): File {
        val suffix = when (contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }
        val tempFile = File.createTempFile("profile_", suffix, cacheDir)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        return tempFile
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}


