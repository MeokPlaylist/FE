package com.meokpli.app.user

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.meokpli.app.R
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var birthEdit: EditText
    private lateinit var errorMsg: TextView
    private lateinit var nextBtn: Button
    private lateinit var api: UserApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // View 연결
        emailEdit = findViewById(R.id.editTextEmail)
        nameEdit = findViewById(R.id.editTextName)
        birthEdit = findViewById(R.id.editTextBirth)
        errorMsg = findViewById(R.id.textViewError)
        nextBtn = findViewById(R.id.btnNext)

        // Retrofit 초기화
        api = Retrofit.Builder()
            .baseUrl("https://meokplaylist.store/user/") // 실제 서버 주소
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

        nextBtn.setOnClickListener {
            handleNext()
        }
    }

    private fun handleNext() {
        val email = emailEdit.text.toString().trim()
        val name = nameEdit.text.toString().trim()
        val birth = birthEdit.text.toString().trim()

        errorMsg.visibility = View.GONE

        if (email.isBlank() || name.isBlank()) {
            showError("이메일과 이름은 필수입니다.")
            return
        }

        val request = FindUserRequest(name, email)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.findUser(request)  // 성공 시 userId만 반환됨
                val userId = response.userId

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("정보가 일치하지 않습니다.")
                }
            }
        }
    }

    private fun showError(msg: String) {
        errorMsg.text = msg
        errorMsg.visibility = View.VISIBLE
    }
}
