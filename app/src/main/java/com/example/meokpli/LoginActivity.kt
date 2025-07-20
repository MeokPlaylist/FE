package com.example.meokpli

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var prefs: android.content.SharedPreferences
    lateinit var api: AuthApi

    val useServer = true  // 서버 연동시 true로 변경

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("meokpli_prefs", MODE_PRIVATE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("614172335108-a6j4nkrudna9k8tpon4anj3jgi6ee0ts.apps.googleusercontent.com") // 나중에 서버 연동 시 사용, 현재는 필요 없음
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        api = Retrofit.Builder()
            .baseUrl("https://your.api.server/")  // 나중에 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        val emailEdit = findViewById<EditText>(R.id.emailEdit)
        val pwEdit = findViewById<EditText>(R.id.passwordEdit)

        // 회원가입
        findViewById<Button>(R.id.registerButton).setOnClickListener {
            val email = emailEdit.text.toString()
            val pw = pwEdit.text.toString()

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val res = api.register(RegisterRequest(email, pw, email))
                        goMain("서버 회원가입 성공: ${res.message}")
                    } catch (e: Exception) {
                        showError("회원가입 실패: ${e.message}")
                    }
                }
            } else {
                prefs.edit().putString(email, pw).apply()
                Toast.makeText(this, "로컬 회원가입 완료", Toast.LENGTH_SHORT).show()
            }
        }

        // 자체 로그인
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val email = emailEdit.text.toString()
            val pw = pwEdit.text.toString()

            if (useServer) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val res = api.login(LoginRequest(email, pw))
                        goMain("서버 로그인 성공: ${res.message}")
                    } catch (e: Exception) {
                        showError("로그인 실패: ${e.message}")
                    }
                }
            } else {
                val savedPw = prefs.getString(email, null)
                if (savedPw != null && savedPw == pw) {
                    goMain("로컬 로그인 성공: $email")
                } else {
                    showError("로컬 로그인 실패")
                }
            }
        }

        // 구글 로그인
        findViewById<Button>(R.id.googleButton).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1000)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                if (account != null) {
                    val email = account.email
                    val name = account.displayName
                    val idToken = account.idToken

                    if (useServer) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val res = api.oauthLogin(OAuthRequest("google", idToken!!))
                                goMain("서버 구글 로그인 성공: ${res.message}")
                            } catch (e: Exception) {
                                showError("구글 로그인 실패: ${e.message}")
                            }
                        }
                    } else {
                        goMain("구글 로그인 (로컬 모드): $name ($email)")
                    }
                } else {
                    showError("구글 계정 정보 가져오기 실패")
                }
            } catch (e: Exception) {
                showError("구글 로그인 예외: ${e.message}")
            }
        }
    }

    private fun goMain(info: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userInfo", info)
        startActivity(intent)
        finish()
    }

    private fun showError(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}