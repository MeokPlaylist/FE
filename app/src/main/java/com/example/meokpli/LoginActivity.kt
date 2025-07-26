package com.example.meokpli

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                if (account != null) {
                    val idToken = account.idToken
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
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
