package com.example.meokpli.User

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.Auth.Network
import com.example.meokpli.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ChangePasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_FORGOT_FLOW = "extra_forgot_flow"
    }

    private val userApi by lazy { Network.userApi(this) } // /user/*
    private val authApi by lazy { Network.authApi(this) } // /auth/* (현재 비밀번호 검증용) :contentReference[oaicite:10]{index=10}

    private var userId: Long = -1L
    private var email: String? = null
    private var name: String? = null
    private var isForgotFlow: Boolean = false

    private lateinit var btnBack: ImageButton
    private lateinit var btnSubmit: Button
    private lateinit var groupCurrentSection: View
    private lateinit var etCurrent: EditText
    private lateinit var etNew: EditText
    private lateinit var etConfirm: EditText
    private lateinit var tvErrCurrent: TextView
    private lateinit var tvErrNew: TextView
    private lateinit var tvErrConfirm: TextView
    private lateinit var tvFindPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password) // xml 기반 :contentReference[oaicite:11]{index=11}

        // View refs
        btnBack = findViewById(R.id.btnBack)
        btnSubmit = findViewById(R.id.btnSubmit)
        groupCurrentSection = findViewById(R.id.groupCurrentSection) // ★ XML에서 추가한 그룹 id
        etCurrent = findViewById(R.id.etCurrentPassword)
        etNew = findViewById(R.id.etNewPassword)
        etConfirm = findViewById(R.id.etConfirmPassword)
        tvErrCurrent = findViewById(R.id.tvCurrentError)
        tvErrNew = findViewById(R.id.tvNewError)
        tvErrConfirm = findViewById(R.id.tvConfirmError)
        tvFindPassword = findViewById(R.id.tvFindPassword)

        // Intent extras
        userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        email = intent.getStringExtra(EXTRA_EMAIL)
        name = intent.getStringExtra(EXTRA_NAME)
        isForgotFlow = intent.getBooleanExtra(EXTRA_FORGOT_FLOW, false)

        // 분실 플로우면 현재 비밀번호 섹션 숨김
        if (isForgotFlow) groupCurrentSection.visibility = View.GONE

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvFindPassword.setOnClickListener {
            val intent = Intent(this, PasswordForgotActivity::class.java)
            startActivity(intent)
        }

        // 실시간 검증
        etCurrent.doAfterTextChanged { validateAll() }   // 일반 변경 플로우에서만 의미
        etNew.doAfterTextChanged { validateAll() }
        etConfirm.doAfterTextChanged { validateAll() }
        validateAll()

        btnSubmit.setOnClickListener {
            hideKeyboard()
            if (validateAll()) submit()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService<InputMethodManager>()
        currentFocus?.windowToken?.let { imm?.hideSoftInputFromWindow(it, 0) }
    }

    /** 비밀번호 규칙: 8~16자, 영문 대/소/숫자/특수문자 각 1+ */
    private fun isValidPw(pw: String): Boolean {
        if (pw.length !in 8..16) return false
        val hasUpper = pw.any { it.isUpperCase() }
        val hasLower = pw.any { it.isLowerCase() }
        val hasDigit = pw.any { it.isDigit() }
        val hasSpecial = pw.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    /** 실시간 검증 + 에러 메시지 + 버튼 활성화 */
    private fun validateAll(): Boolean {
        val current = etCurrent.text?.toString().orEmpty()
        val newPw = etNew.text?.toString().orEmpty()
        val confirm = etConfirm.text?.toString().orEmpty()

        // 현재 비번: 분실 플로우가 아니면 비어 있으면 경고(형식 제약은 두지 않음)
        if (!isForgotFlow) {
            tvErrCurrent.text = "현재 비밀번호를 입력하세요."
            tvErrCurrent.visibility = if (current.isEmpty()) View.VISIBLE else View.GONE
        } else {
            tvErrCurrent.visibility = View.GONE
        }

        // 새 비번: 규칙 + 현재와 동일 금지
        val sameAsCurrent = (!isForgotFlow) && newPw.isNotEmpty() && (newPw == current)
        tvErrNew.text = when {
            sameAsCurrent -> "현재 비밀번호와 다른 비밀번호를 사용해 주세요."
            else -> "8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
        }
        tvErrNew.visibility = if (newPw.isNotEmpty() && (!isValidPw(newPw) || sameAsCurrent)) View.VISIBLE else View.GONE

        // 확인: 일치 여부
        tvErrConfirm.text = when {
            confirm.isEmpty() -> "새 비밀번호와 확인을 입력하세요."
            confirm != newPw -> "비밀번호가 일치하지 않습니다."
            else -> ""
        }
        tvErrConfirm.visibility = if (confirm.isEmpty() || confirm != newPw) View.VISIBLE else View.GONE

        // 버튼 활성화 조건
        val okCurrent = isForgotFlow || current.isNotEmpty()
        val okNew = isValidPw(newPw) && (!sameAsCurrent)
        val okConfirm = confirm.isNotEmpty() && (confirm == newPw)

        val enabled = okCurrent && okNew && okConfirm
        btnSubmit.isEnabled = enabled
        return enabled
    }

    /** 제출: (일반) 현재 비번 검증 → userId 보장 → 변경 / (분실) userId 보장 → 변경 */
    private fun submit() {
        lifecycleScope.launch {
            try {
                // 0) 이메일/이름/userId 확보 (userId는 반드시 서버에 같이 보냄)
                var uid = this@ChangePasswordActivity.userId
                var em = this@ChangePasswordActivity.email
                var nm = this@ChangePasswordActivity.name

                if (em.isNullOrBlank() || nm.isNullOrBlank() || uid <= 0L) {
                    // 내 정보 가져오기 (email/name)
                    val me = withContext(Dispatchers.IO) { userApi.getPersonalInfo() } // :contentReference[oaicite:12]{index=12}
                    if (em.isNullOrBlank()) em = me.email
                    if (nm.isNullOrBlank()) nm = me.name
                    // userId 없으면 /find로 조회
                    if (uid <= 0L && !nm.isNullOrBlank() && !em.isNullOrBlank()) {
                        uid = withContext(Dispatchers.IO) { userApi.findUser(FindUserRequest(nm!!, em!!)).userId } // :contentReference[oaicite:13]{index=13}
                    }
                }

                if (uid <= 0L || em.isNullOrBlank()) {
                    Toast.makeText(this@ChangePasswordActivity, "사용자 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 1) 일반 플로우: 현재 비밀번호 검증 (로그인 시도) — 분실 플로우면 스킵
                if (!isForgotFlow) {
                    val current = etCurrent.text.toString()
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            // /auth/login(email, password) 성공(200)이면 현재 비번 일치로 판단
                            authApi.login(com.example.meokpli.Auth.LoginRequest(em!!, current))
                            true
                        } catch (e: HttpException) {
                            (e.code() != 400 && e.code() != 401).also { if (!it) { /* 400/401 → 틀림 */ } }
                        }
                    }
                    if (!ok) {
                        tvErrCurrent.text = "현재 비밀번호가 올바르지 않습니다."
                        tvErrCurrent.visibility = View.VISIBLE
                        return@launch
                    }
                }

                // 2) 새 비밀번호 변경 (항상 userId 동봉)
                val newPw = etNew.text.toString()
                withContext(Dispatchers.IO) {
                    userApi.resetPassword(ResetPasswordRequest(uid, newPw)) // Unit 반환(성공 시 예외 없음) :contentReference[oaicite:14]{index=14}
                }

                Toast.makeText(this@ChangePasswordActivity, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChangePasswordActivity, "변경에 실패했습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
