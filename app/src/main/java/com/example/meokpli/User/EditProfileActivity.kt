package com.example.meokpli.User

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.R
import com.example.meokpli.Auth.Network
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvChangePhoto: TextView
    private lateinit var etNickname: EditText
    private lateinit var etBio: EditText
    private lateinit var tvNickCount: TextView
    private lateinit var tvBioCount: TextView
    private lateinit var btnSubmit: Button

    private lateinit var api: UserApi

    private var selectedImageUri: Uri? = null
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivAvatar.setImageURI(it) // 미리보기만
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        api = Network.userApi(this)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvChangePhoto = findViewById(R.id.tvChangePhoto)
        etNickname = findViewById(R.id.etNickname)
        etBio = findViewById(R.id.etBio)
        tvNickCount = findViewById(R.id.tvNickCount)
        tvBioCount = findViewById(R.id.tvBioCount)
        btnSubmit = findViewById(R.id.btnSubmit)

        // 뒤로가기
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 사진 선택(미리보기만)
        val openPicker = { pickImage.launch("image/*") }
        ivAvatar.setOnClickListener { openPicker() }
        tvChangePhoto.setOnClickListener { openPicker() }

        // 글자수 카운트 (닉네임 10자, 소개 20자)
        etNickname.addTextChangedListener(counterWatcher { c -> tvNickCount.text = "$c / 10" })
        etBio.addTextChangedListener(counterWatcher { c -> tvBioCount.text = "$c / 20" })

        // 저장
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
                val body = UserDetailRequest(nickname = nick, introduction = bio)
                api.saveDetail(body)

                Toast.makeText(this@EditProfileActivity, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditProfileActivity, "저장에 실패했습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = "변경완료"
            }
        }
    }
}
