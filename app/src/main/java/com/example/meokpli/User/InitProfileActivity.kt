package com.example.meokpli.User

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.Auth.LoginActivity
import com.example.meokpli.Auth.Network
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import com.example.meokpli.R
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

    private val PICK_IMAGE_REQUEST = 1
    private val NICKNAME_LIMIT = 10
    private val INTRO_LIMIT = 30

    private lateinit var api: UserApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.init_profile)

        imageProfile = findViewById(R.id.imageProfile)
        editNickname = findViewById(R.id.editNickname)
        textNicknameCount = findViewById(R.id.textNicknameCount)
        textIntroCount = findViewById(R.id.textIntroCount)
        editIntro = findViewById(R.id.editIntro)
        buttonNext = findViewById(R.id.buttonNext)

        api = Network.userApi(this)

        // Retrofit 초기화
        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/user/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

        // 글자수 제한 필터 적용 (UX)
        editNickname.filters = arrayOf(InputFilter.LengthFilter(NICKNAME_LIMIT))
        editIntro.filters = arrayOf(InputFilter.LengthFilter(INTRO_LIMIT))

        // 닉네임 글자 수 실시간 표시
        editNickname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                textNicknameCount.text = "$count/$NICKNAME_LIMIT"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        //소개글 글자수 없데이트
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

        // 다음 버튼 클릭 시 서버 저장
        buttonNext.setOnClickListener {
            val nickname = editNickname.text.toString().trim()
            val intro = editIntro.text.toString().trim()

            if (nickname.isEmpty()) {
                showToast("닉네임을 입력해주세요.")
                return@setOnClickListener
            }
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    // ✅ 헤더 전달 불필요 (인터셉터가 Bearer 자동 부착)
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

                val file = createTempFileFromUriSafe(imageUri) // ← 수정된 안전 함수
                val part = MultipartBody.Part.createFormData(
                    name = "profileImg", // 서버 필드명과 동일해야 함
                    filename = file.name,
                    body = file.asRequestBody("image/*".toMediaTypeOrNull())
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        // ✅ 헤더 전달 불필요
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

