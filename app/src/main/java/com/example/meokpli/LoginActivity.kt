package com.example.meokpli

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.SignInButton
import com.example.meokpli.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val rcSignIn = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.server_client_id)) // ← 필요 없다면 삭제
            .build()

        // Client 생성
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 버튼 사이즈/색상 옵션
        binding.btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)

        // 클릭 시 로그인 플로우 시작
        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, rcSignIn)
        }
    }

    // 결과 콜백
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcSignIn) {
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // ✅ ID Token 출력
            val idToken = account.idToken
            Log.d("GOOGLE_ID_TOKEN", "ID Token: $idToken")

            // 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()

        } catch (e: ApiException) {
            Log.e("GOOGLE_LOGIN", "signInResult:failed code=${e.statusCode}", e)
        }
    }
}
