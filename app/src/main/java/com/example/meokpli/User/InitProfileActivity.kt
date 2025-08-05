package com.example.meokpli.User

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

        // Retrofit 초기화
        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/user/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

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
            val nickname = editNickname.text.toString()
            val intro = editIntro.text.toString()

            if (nickname.isBlank()) {
                showToast("닉네임을 입력해주세요.")
                return@setOnClickListener
            }
            if (nickname.length > NICKNAME_LIMIT) {
                showToast("닉네임은 최대 10자까지 가능합니다.")
                return@setOnClickListener
            }
            if (intro.length > INTRO_LIMIT) {
                showToast("소개는 최대 30자까지 입력 가능합니다.")
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val jwt = getSharedPreferences("meokpli_prefs", MODE_PRIVATE).getString("jwt_token", null)
                    if (jwt.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            showToast("JWT가 존재하지 않습니다.")
                        }
                        return@launch
                    }

                    val bearerToken = "Bearer $jwt"
                    api.saveDetail(bearerToken, UserDetailRequest(nickname, intro)) // 응답 무시
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@InitProfileActivity, CategoryActivity::class.java)
                        startActivity(intent)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            try {

                val inputStream: InputStream? = imageUri?.let { contentResolver.openInputStream(it) }
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageProfile.setImageBitmap(bitmap)

                // ✅ 이미지 서버 전송
                imageUri?.let { uri ->
                    val file = createTempFileFromUri(uri)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("profileImg", file.name, requestFile)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val jwt = getSharedPreferences("meokpli_prefs", MODE_PRIVATE).getString("jwt_token", null)
                            if (jwt.isNullOrBlank()) {
                                withContext(Dispatchers.Main) {
                                    showToast("JWT가 존재하지 않습니다.")
                                }
                                return@launch
                            }
                            val bearerToken = "Bearer $jwt"
                            api.savePhoto(bearerToken, UserPhotoRequest(body))
                            withContext(Dispatchers.Main) {
                                showToast("이미지 업로드 성공!")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                showToast("업로드 실패: ${e.message}")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                showToast("이미지를 불러오지 못했습니다.")
            }
        }
    }

    fun Context.createTempFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("InputStream null")
        val file = this@InitProfileActivity.createTempFileFromUri(uri)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)

        outputStream.close()
        inputStream.close()

        return file
    }


    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
