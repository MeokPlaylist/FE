package com.meokpli.app.user

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.meokpli.app.auth.Network
import com.meokpli.app.R
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PasswordForgotActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etBirth: EditText
    private lateinit var etEmail: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var btnNext: Button
    private lateinit var btnBack: ImageButton

    private val userApi by lazy { Network.userApi(this) } // /user/* API 인스턴스 (지연 초기화)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 주의: 프로젝트에 올라온 파일명 오타 그대로 사용 (actiyity_password_forgot.xml)
        setContentView(R.layout.actiyity_password_forgot) // ← xml 파일명 :contentReference[oaicite:5]{index=5}

        etName = findViewById(R.id.etName)
        etBirth = findViewById(R.id.etBirth)
        etEmail = findViewById(R.id.etEmail)
        tvEmailError = findViewById(R.id.tvEmailError)
        btnNext = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        // 실시간 이메일 형식 체크 + 버튼 활성화
        etName.doAfterTextChanged { refreshButtonState() }
        etBirth.doAfterTextChanged { refreshButtonState() }
        etEmail.doAfterTextChanged {
            val email = etEmail.text.toString().trim()
            tvEmailError.visibility = if (email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()) View.GONE else View.VISIBLE
            refreshButtonState()
        }

        btnNext.setOnClickListener { onNext() }
        refreshButtonState()
    }

    private fun refreshButtonState() {
        val nameOk = etName.text.toString().trim().isNotEmpty()
        val birthOk = etBirth.text.toString().trim().isNotEmpty() // 서버는 find에 birth를 안 쓰지만 화면상 필수 입력
        val email = etEmail.text.toString().trim()
        val emailOk = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        btnNext.isEnabled = nameOk && birthOk && emailOk
    }

    private fun onNext() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()

        lifecycleScope.launch {
            try {
                // 1) /user/find → userId 획득
                val res = userApi.findUser(FindUserRequest(name, email)) // :contentReference[oaicite:6]{index=6}
                val userId = res.userId

                // 2) ChangePasswordActivity로 이동 (분실 플로우 플래그 포함)
                val intent = Intent(this@PasswordForgotActivity, ChangePasswordActivity::class.java).apply {
                    putExtra(ChangePasswordActivity.EXTRA_USER_ID, userId)
                    putExtra(ChangePasswordActivity.EXTRA_EMAIL, email)
                    putExtra(ChangePasswordActivity.EXTRA_NAME, name)
                    putExtra(ChangePasswordActivity.EXTRA_FORGOT_FLOW, true) // ★ 분실 플로우
                }
                startActivity(intent)

            } catch (e: Exception) {
                when (e) {
                    is HttpException -> toast("일치하는 회원 정보를 찾을 수 없습니다.")
                    is IOException -> toast("네트워크 오류입니다. 연결을 확인하세요.")
                    else -> toast("오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                }
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
