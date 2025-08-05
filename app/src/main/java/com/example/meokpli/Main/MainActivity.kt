package com.example.meokpli.Main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.Auth.LoginActivity
import com.example.meokpli.R
import com.example.meokpli.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 환영 메시지
        val account = GoogleSignIn.getLastSignedInAccount(this)
        binding.tvWelcome.text = "환영합니다! ${account?.email ?: ""}"

        // 로그아웃 버튼 클릭
        binding.btnLogout.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id)) // 서버와 연동할 경우
                .build()

            val signInClient = GoogleSignIn.getClient(this, gso)

            // ✅ 로그아웃 수행
            signInClient.signOut().addOnCompleteListener {
                Log.d("GOOGLE_LOGOUT", "로그아웃 완료")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
