package com.example.meokpli.Auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.R
import com.example.meokpli.User.ForgotPasswordActivity
import com.example.meokpli.Main.MainActivity
import com.example.meokpli.User.ConsentFormActivity
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
        val kakaoButton = findViewById<ImageView>(R.id.btnKakao)

        prefs = getSharedPreferences("meokpli_prefs", MODE_PRIVATE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("614172335108-a6j4nkrudna9k8tpon4anj3jgi6ee0ts.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/auth/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        registerButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        val findInfoButton = findViewById<TextView>(R.id.tvFindInfo)
        findInfoButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val pw = passwordEdit.text.toString()

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val res = api.login(LoginRequest(email, pw))
                        saveJwtToken(res.jwt)
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

        // 🔵 구글 로그인 버튼
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1000)
        }

        // 🟡 카카오 로그인 버튼
        kakaoButton.setOnClickListener {
            kakaoLogin()
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
                                saveJwtToken(res.jwt)
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
                            val res = api.oauthLogin(OAuthRequest("kakao", idToken!!))
                            Log.d("JWT_TOKEN", "Kakao Login JWT: ${res.jwt}")
                            saveJwtToken(res.jwt)
                            goMain()
                        } catch (e: Exception) {
                            showError("카카오 로그인 실패: ${e.message}")
                        }
                    }
                } else {
                    goMain()
                }
            }
        }
        // 카카오톡 설치 여부 확인 후 로그인 실행
        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
    }

    private fun goMain() {
        runOnUiThread {
            startActivity(Intent(this, ConsentFormActivity::class.java))
            finish()
        }
    }

    private fun showError(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveJwtToken(token: String) {
        val editor = prefs.edit()
        editor.putString("jwt_token", token)
        editor.apply()
    }
}