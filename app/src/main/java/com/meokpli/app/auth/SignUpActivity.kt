package com.meokpli.app.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.meokpli.app.R
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUpActivity : AppCompatActivity() {

    private lateinit var pwEdit: EditText
    private lateinit var confirmEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var birthEdit: EditText
    private lateinit var registerBtn: Button
    private lateinit var checkEmailBtn: Button
    private lateinit var emailMsg: TextView
    private lateinit var pwMsg: TextView
    private var isEmailChecked = false
    private lateinit var api: AuthApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)



        // View 연결
        pwEdit = findViewById(R.id.editTextPassword)
        confirmEdit = findViewById(R.id.editTextConfirm)
        emailEdit = findViewById(R.id.editTextEmail)
        nameEdit = findViewById(R.id.editTextName)
        birthEdit = findViewById(R.id.editTextBirth)
        registerBtn = findViewById(R.id.btnRegister)
        checkEmailBtn = findViewById(R.id.btnCheckEmail)

        emailMsg = findViewById(R.id.textViewEmailMsg)
        pwMsg = findViewById(R.id.textViewPwMsg)

        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/auth/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        checkEmailBtn.setOnClickListener {
            val email = emailEdit.text.toString()
            if (email.isBlank()) {
                emailMsg.text = "이메일을 입력해주세요."
                emailMsg.setTextColor(0xFFDF4A4A.toInt())
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = api.checkEmail(EmailInspectRequest(email))
                    withContext(Dispatchers.Main) {
                        if (res.isAvailable) {
                            emailMsg.text = "사용 가능한 이메일입니다."
                            emailMsg.setTextColor(0xFF3CB371.toInt())
                            emailEdit.isEnabled = false
                            checkEmailBtn.isEnabled = false
                            isEmailChecked = true
                        } else {
                            emailMsg.text = "이미 사용 중인 이메일입니다."
                            emailMsg.setTextColor(0xFFDF4A4A.toInt())
                            isEmailChecked = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        emailMsg.text = "잘못된 형식의 이메일입니다."
                        emailMsg.setTextColor(0xFFDF4A4A.toInt())
                    }
                }
            }
        }

        pwEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pw = s.toString()
                val pwRegex =
                    Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,16}$")
                if (!pw.matches(pwRegex)) {
                    pwMsg.text = "8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
                    pwMsg.setTextColor(0xFFDF4A4A.toInt())
                } else {
                    pwMsg.text = ""
                }
            }
        })

        registerBtn.setOnClickListener {
            handleRegister()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleRegister() {
        if (!isEmailChecked) {
            showToast("이메일 중복 확인을 해주세요.")
            return
        }
        val pw = pwEdit.text.toString()
        val confirm = confirmEdit.text.toString()
        val email = emailEdit.text.toString()
        val name = nameEdit.text.toString()
        val birth = birthEdit.text.toString()

        pwMsg.text = ""
        if (pw.isBlank() || confirm.isBlank() || email.isBlank() || name.isBlank()) {
            showToast("필수 항목을 모두 입력해 주세요.")
            return
        }

        if (pw != confirm) {
            pwMsg.text = "비밀번호가 일치하지 않습니다."
            pwMsg.setTextColor(0xFFDF4A4A.toInt())
            return
        }

        val pwRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,16}$")
        if (!pw.matches(pwRegex)) {
            pwMsg.text = "8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
            pwMsg.setTextColor(0xFFDF4A4A.toInt())
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