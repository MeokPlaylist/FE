package com.example.meokpli

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.AuthApi
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUpActivity : AppCompatActivity() {

    private lateinit var idEdit: EditText
    private lateinit var pwEdit: EditText
    private lateinit var confirmEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var birthEdit: EditText
    private lateinit var registerBtn: Button

    private lateinit var api: AuthApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup) // XML 이름이 signup.xml인 경우

        // View 연결
        idEdit = findViewById(R.id.editTextId)
        pwEdit = findViewById(R.id.editTextPassword)
        confirmEdit = findViewById(R.id.editTextConfirm)
        emailEdit = findViewById(R.id.editTextEmail)
        nameEdit = findViewById(R.id.editTextName)
        birthEdit = findViewById(R.id.editTextBirth)
        registerBtn = findViewById(R.id.btnRegister)

        // Retrofit 초기화
        api = Retrofit.Builder()
            .baseUrl("https://your.api.server/") // 🔴 실제 API 주소로 교체
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        registerBtn.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val id = idEdit.text.toString()
        val pw = pwEdit.text.toString()
        val confirm = confirmEdit.text.toString()
        val email = emailEdit.text.toString()
        val nickname = nameEdit.text.toString()
        val birth = birthEdit.text.toString()

        // 유효성 검사
        if (id.isBlank() || pw.isBlank() || confirm.isBlank() || email.isBlank() || nickname.isBlank()) {
            showToast("필수 항목을 모두 입력해 주세요.")
            return
        }

        if (pw != confirm) {
            showToast("비밀번호가 일치하지 않습니다.")
            return
        }

        if (pw.length < 8 || !pw.matches(Regex(".*[!@#\$%^&*(),.?\":{}|<>].*"))) {
            showToast("비밀번호는 8자 이상이며 특수문자를 포함해야 합니다.")
            return
        }

        // 서버 연동
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.register(RegisterRequest(email, pw, nickname, birth))
                withContext(Dispatchers.Main) {
                    showToast("회원가입 성공: ${res.message}")
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("회원가입 실패: ${e.message}")
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
