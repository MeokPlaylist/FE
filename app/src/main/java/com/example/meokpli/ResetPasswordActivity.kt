package com.example.meokpli

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var pwErrorMsg: TextView
    private lateinit var confirmPwErrorMsg: TextView
    private lateinit var btnReset: Button

    private lateinit var api: AuthApi

    private val pwRegex =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\\\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\\\$%^&*(),.?\":{}|<>]{8,16}")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        editPassword = findViewById(R.id.editTextPassword)
        editConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        pwErrorMsg = findViewById(R.id.pwErrorMessage)
        confirmPwErrorMsg = findViewById(R.id.confirmPwErrorMessage)
        btnReset = findViewById(R.id.btnResetPassword)

        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/user/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        // 비밀번호 형식 실시간 체크만 적용 (일치 여부는 버튼 클릭 시만 확인)
        editPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pw = s.toString()
                if (!pw.matches(pwRegex)) {
                    pwErrorMsg.text = "8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
                } else {
                    pwErrorMsg.text = ""
                }
            }
        })

        btnReset.setOnClickListener {
            if (validateInput()) {
                changePassword()
            }
        }
    }

    private fun validateInput(): Boolean {
        val pw = editPassword.text.toString()
        val confirmPw = editConfirmPassword.text.toString()

        var isValid = true

        if (!pw.matches(pwRegex)) {
            pwErrorMsg.text = "8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
            isValid = false
        } else {
            pwErrorMsg.text = ""
        }

        if (pw != confirmPw) {
            confirmPwErrorMsg.text = "비밀번호가 일치하지 않습니다."
            isValid = false
        } else {
            confirmPwErrorMsg.text = ""
        }

        return isValid
    }

    private fun changePassword() {
        val newPassword = editPassword.text.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.resetPassword(ResetPasswordRequest(newPassword))
                runOnUiThread {
                    Toast.makeText(this@ResetPasswordActivity, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    goLoginActivity()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ResetPasswordActivity, "비밀번호 변경 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}


