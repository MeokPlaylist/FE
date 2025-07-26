package com.example.meokpli

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.appcompat.widget.Toolbar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SignUpActivity : AppCompatActivity() {

    private lateinit var pwEdit: EditText
    private lateinit var confirmEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var birthEdit: EditText
    private lateinit var registerBtn: Button
    private lateinit var checkEmailBtn: Button

    private lateinit var api: AuthApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // ← 버튼 동작 추가
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // 텍스트는 안 보이게 (원하면 true)

        toolbar.setNavigationOnClickListener {
            finish() // 뒤로가기 동작
        }

        // View 연결
        pwEdit = findViewById(R.id.editTextPassword)
        confirmEdit = findViewById(R.id.editTextConfirm)
        emailEdit = findViewById(R.id.editTextEmail)
        nameEdit = findViewById(R.id.editTextName)
        birthEdit = findViewById(R.id.editTextBirth)
        registerBtn = findViewById(R.id.btnRegister)
        checkEmailBtn = findViewById(R.id.btnCheckEmail)

        // Retrofit 초기화
        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        // 회원가입 버튼
        registerBtn.setOnClickListener {
            handleRegister()
        }

        // 이메일 중복 확인 버튼
        checkEmailBtn.setOnClickListener {
            val email = emailEdit.text.toString()
            if (email.isBlank()) {
                showToast("이메일을 입력해주세요.")
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = api.checkEmail(emailInspectRequest(email))
                    withContext(Dispatchers.Main) {
                        if (res.isAvailable) {
                            showToast("사용 가능한 이메일입니다.")
                            emailEdit.isEnabled = false  // ✅ 입력 비활성화
                            checkEmailBtn.isEnabled = false // ✅ 버튼도 비활성화 (선택)
                        } else {
                            showToast("이미 사용 중인 이메일입니다.")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("중복 확인 실패: ${e.message}")
                    }
                }
            }
        }
    }

    // ✅ 뒤로가기 버튼 클릭 시 동작
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 현재 화면 종료
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleRegister() {
        val pw = pwEdit.text.toString()
        val confirm = confirmEdit.text.toString()
        val email = emailEdit.text.toString()
        val name = nameEdit.text.toString()
        val birth = birthEdit.text.toString()

        if (pw.isBlank() || confirm.isBlank() || email.isBlank() || name.isBlank()) {
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.register(RegisterRequest(email, pw, name, birth))
                withContext(Dispatchers.Main) {
                    showToast("회원가입 성공")
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
