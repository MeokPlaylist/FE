package com.example.meokpli.Auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.R
import com.example.meokpli.User.CategoryActivity
import com.example.meokpli.User.ForgotPasswordActivity
import com.example.meokpli.User.ConsentFormActivity
import com.example.meokpli.User.InitProfileActivity
import com.google.android.gms.auth.api.signin.*
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import retrofit2.HttpException
import com.example.meokpli.common.UserNotFoundDialogFragment
import kotlinx.coroutines.*
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var api: AuthApi
    private lateinit var tokenManager: TokenManager

    private val useServer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(this)
        api = Network.authApi(this)

        val emailEdit = findViewById<EditText>(R.id.editTextId)
        val passwordEdit = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<TextView>(R.id.tvSignUp)
        val googleButton = findViewById<ImageView>(R.id.btnGoogle)
        val kakaoButton = findViewById<ImageView>(R.id.btnKakao)
        val findInfoButton = findViewById<TextView>(R.id.tvFindInfo)
        val btnKeepLogin = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnKeepLogin)

        // 구글 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("614172335108-a6j4nkrudna9k8tpon4anj3jgi6ee0ts.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 로그인 유지 토글 UI 반영
        btnKeepLogin.isSelected = tokenManager.isKeepLogin()
        btnKeepLogin.setOnClickListener {
            val newChecked = !btnKeepLogin.isSelected
            btnKeepLogin.isSelected = newChecked
            tokenManager.setKeepLogin(newChecked)
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        findInfoButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val pw = passwordEdit.text.toString()
            if (email.isBlank() || pw.isBlank()) {
                showError("아이디와 비밀번호를 입력해 주세요")
                return@setOnClickListener
            }

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val res = api.login(LoginRequest(email, pw))
                        tokenManager.saveTokens(res.accessToken, res.refreshToken)
                        routeAfterLoginByErrorCodeOnly()
                    } catch (e: Exception) {
                        var notFound = false
                        if (e is HttpException) {
                            //코드만 보내는지 확인
                            val (codeStr, _) = parseError(e)
                            if (e.code() == 404 || codeStr == "USER_NOT_FOUND") {
                                notFound = true
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (notFound) {
                                UserNotFoundDialogFragment
                                    .new(
                                        title = "로그인에 실패하였습니다.",
                                        message = "존재하지 않는 아이디입니다.\n아이디/비밀번호 확인 후 다시 시도해주세요."
                                    )
                                    .show(supportFragmentManager, "user_not_found")
                            } else {
                                showError("로그인 실패: ${e.message}")
                            }
                        }
                    }

                }
            } else {
                showError("로컬 로그인은 비활성화됨")
            }
        }

        // 🔵 구글 로그인 버튼
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, REQ_GOOGLE)
        }

        // 🟡 카카오 로그인 버튼
        kakaoButton.setOnClickListener { kakaoLogin() }
    }

    override fun onStart() {
        super.onStart()
        // 자동 진입: 유지가 켜져 있고 refresh 가 있으면 가볍게 상태 호출 → 필요 시 자동 refresh
        if (tokenManager.isKeepLogin() && !tokenManager.getRefreshToken().isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    routeAfterLoginByErrorCodeOnly()
                } catch (_: Exception) {
                    // 실패 시 로그인 화면에 그대로 둔다
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken == null) {
                        showError("구글 로그인 토큰을 가져오지 못했습니다")
                        return
                    }
                    if (useServer) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val res = api.oauthLogin(OAuthRequest("google", idToken))
                                tokenManager.saveTokens(res.accessToken, res.refreshToken)
                                routeAfterLoginByErrorCodeOnly()
                            } catch (e: Exception) {
                                showError("구글 로그인 실패: ${e.message}")
                            }
                        }
                    } else {
                        goNext()
                    }
                } else {
                    showError("구글 계정 정보 가져오기 실패")
                }
            } catch (e: Exception) {
                showError("구글 로그인 예외: ${e.message}")
            }
        }
    }

    private fun kakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                showError("카카오 로그인 실패: ${error.message}")
            } else if (token != null) {
                val idToken = token.accessToken
                Log.d("KAKAO", "AccessToken: $idToken")

                if (useServer) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val res = api.oauthLogin(OAuthRequest("kakao", idToken))
                            tokenManager.saveTokens(res.accessToken, res.refreshToken)
                            routeAfterLoginByErrorCodeOnly()
                        } catch (e: Exception) {
                            showError("카카오 로그인 실패: ${e.message}")
                        }
                    }
                } else {
                    goNext()
                }
            }
        }
        // 카카오톡 설치 여부와 무관하게 계정 로그인 사용
        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
    }
    private suspend fun routeAfterLoginByErrorCodeOnly() {
        try {
            // 보호 API 한 번만 호출 (성공이면 모든 선행조건 충족으로 간주)
            api.getProfileStatus()
            goNext()
            return
        } catch (e: HttpException) {
            val (codeString, codeNumber) = parseError(e)  // ← 한 번만 읽음

            when {
                codeString == "CONSENT_NOT_FOUND" || codeNumber == 453 -> {
                    withContext(Dispatchers.Main) {
                    startActivity(Intent(this@LoginActivity, ConsentFormActivity::class.java))
                    finish()
                }
                    return
                }
                codeString == "DONT_HAVE_NICKNAME" || codeNumber == 455 -> {
                    withContext(Dispatchers.Main) {
                    startActivity(Intent(this@LoginActivity, InitProfileActivity::class.java))
                    finish()
                }
                    return
                }
                codeString == "CATEGORY_NOT_FOUND" || codeNumber == 470 -> {
                    withContext(Dispatchers.Main) {
                    startActivity(Intent(this@LoginActivity, CategoryActivity::class.java))
                    finish()
                }
                    return
                }
                else -> {
                    showOnMain("상태 확인 실패: ${e.message()}")
                    return
                }

            }
        } catch (e: Exception) {
            showOnMain("상태 확인 오류: ${e.message}")
        }
    }
    private fun parseError(e: HttpException): Pair<String?, Int?> {
        return try {
            val raw = e.response()?.errorBody()?.string()
            if (raw.isNullOrBlank()) return null to null
            val json = JSONObject(raw)

            val codeStr = json.optString("code", null) // "CONSENT_NOT_FOUND" 등
            val codeNum = when {
                json.has("status") -> json.optInt("status")
                json.has("code") && json.opt("code") is Number -> (json.opt("code") as Number).toInt()
                else -> null
            }
            codeStr to codeNum
        } catch (_: Exception) { null to null }
    }

    private fun goNext() {
        runOnUiThread {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private fun showOnMain(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun showError(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        private const val REQ_GOOGLE = 1000
    }
}
