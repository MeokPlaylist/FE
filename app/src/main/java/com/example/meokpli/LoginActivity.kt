package com.example.meokpli

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.common.KakaoSdk
import com.google.android.gms.auth.api.signin.*
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var api: AuthApi

    private val useServer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEdit = findViewById<EditText>(R.id.editTextId)
        val passwordEdit = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<TextView>(R.id.tvSignUp)
        val googleButton = findViewById<ImageView>(R.id.btnGoogle)

        prefs = getSharedPreferences("meokpli_prefs", MODE_PRIVATE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("614172335108-a6j4nkrudna9k8tpon4anj3jgi6ee0ts.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        KakaoSdk.init(this, "1a0fd1421e84e625979ad2a917b4e262")

        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        registerButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val pw = passwordEdit.text.toString()

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        api.login(LoginRequest(email, pw))
                        goMain()
                    } catch (e: Exception) {
                        showError("로그인 실패: ${e.message}")
                    }
                }
            } else {
                val savedPw = prefs.getString(email, null)
                if (savedPw != null && savedPw == pw) {
                    goMain()
                } else {
                    showError("로컬 로그인 실패")
                }
            }
        }

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1000)
        }

        val kakaoButton = findViewById<ImageView>(R.id.btnKakao)

        kakaoButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡으로 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    handleKakaoLogin(token, error)
                }
            } else {
                // 카카오계정으로 로그인 (웹뷰)
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    handleKakaoLogin(token, error)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken == null) {
                        Log.d("GoogleLogin", "idToken is null")
                        showError("구글 로그인 토큰을 가져오지 못했습니다")
                        return
                    }
                    if (useServer) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val res = api.oauthLogin(OAuthRequest("google", idToken!!))
                                Log.d("JWT_TOKEN", "Login JWT: ${res.jwt}")
                                goMain()
                            } catch (e: Exception) {
                                showError("구글 로그인 실패: ${e.message}")
                            }
                        }
                    } else {
                        goMain()
                    }
                } else {
                    showError("구글 계정 정보 가져오기 실패")
                }
            } catch (e: Exception) {
                showError("구글 로그인 예외: ${e.message}")
            }
        }
    }

    private fun goMain() {
        Log.d("LoginActivity", "goMain() called")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
    private fun handleKakaoLogin(token: OAuthToken?, error: Throwable?) {
        if (error != null) {
            showError("카카오 로그인 실패: ${error.localizedMessage}")
        } else if (token != null) {
            val idToken = token.idToken // 서버에 전달할 id_token (없을 수도 있음)
            Log.d("KAKAO_LOGIN", "Access Token: ${token.accessToken}")

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val res = api.oauthLogin(OAuthRequest("kakao", token.accessToken))
                        Log.d("JWT_TOKEN", "Login JWT: ${res.jwt}")
                        goMain()
                    } catch (e: Exception) {
                        showError("카카오 로그인 서버 오류: ${e.message}")
                    }
                }
            } else {
                goMain()
            }
        }
    }
}
