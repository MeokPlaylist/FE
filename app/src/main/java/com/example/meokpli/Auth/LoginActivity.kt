package com.example.meokpli.Auth

import android.app.Activity
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
import com.example.meokpli.User.UserApi
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
    private lateinit var userApi: UserApi
    private lateinit var tokenManager: TokenManager

    private val useServer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(this)
        api = Network.authApi(this) // ← Network.kt에서 Authenticator 제거해야 함 (아래 참고)

        val emailEdit = findViewById<EditText>(R.id.editTextId)
        val passwordEdit = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<TextView>(R.id.tvSignUp)
        val googleButton = findViewById<ImageView>(R.id.btnGoogle)
        val kakaoButton = findViewById<ImageView>(R.id.btnKakao)
        val findInfoButton = findViewById<TextView>(R.id.tvFindInfo)
        val btnKeepLogin = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnKeepLogin)

        // 구글 설정 (ID 토큰 필요 시)
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
                        // ✅ Access-only: refreshToken은 더 이상 사용하지 않음
                        tokenManager.saveTokens(res.jwt)
                        routeAfterLoginByErrorCodeOnly()
                    } catch (e: Exception) {
                        var notFound = false
                        if (e is HttpException) {
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
        // ✅ Access-only: keepLogin + accessToken 존재하면 보호 API로 가볍게 진입 확인
        val hasAccess = !tokenManager.getAccessToken().isNullOrBlank()
        if (tokenManager.isKeepLogin() && hasAccess) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    routeAfterLoginByErrorCodeOnly()
                } catch (_: Exception) {
                    // 실패 시 로그인 화면 유지
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
                                tokenManager.saveTokens(res.jwt)
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
        val act: Activity = this
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
                            tokenManager.saveTokens(res.jwt)
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
        UserApiClient.instance.loginWithKakaoAccount(act, callback = callback)
    }

    private suspend fun routeAfterLoginByErrorCodeOnly() {
        try {
            // 보호 API 한 번만 호출 (성공이면 모든 선행조건 충족으로 간주)

            userApi = Network.userApi(this)
            val codeNumber = userApi.newBCheck().code()
            Log.d("sibal", codeNumber.toString())
            when {
                codeNumber == 590 -> {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    return
                }

                codeNumber == 453 -> {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@LoginActivity, ConsentFormActivity::class.java))
                        finish()
                    }
                    return
                }

                codeNumber == 455 -> {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@LoginActivity, InitProfileActivity::class.java))
                        finish()
                    }
                    return
                }

                codeNumber == 456 -> {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@LoginActivity, CategoryActivity::class.java))
                        finish()
                    }
                    return
                }

            }
        } catch (e: HttpException) {

            //에러 발생시 전 화면으로 빠꾸 or 앱 다운

        } catch (e: Exception) {
            showOnMain("상태 확인 오류: ${e.message}")
        }

    }

    private fun parseError(e: HttpException): Pair<String?, Int?> {
        return try {
            val raw = e.response()?.errorBody()?.string()
            if (raw.isNullOrBlank()) return null to null
            val json = JSONObject(raw)

            val codeStr = json.optString("code", null)
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
