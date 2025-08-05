package com.example.meokpli.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.meokpli.ForgotPasswordApi
import com.example.meokpli.ForgotPasswordRequest
import com.example.meokpli.R
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var birthEdit: EditText
    private lateinit var errorMsg: TextView
    private lateinit var nextBtn: Button

    private lateinit var api: ForgotPasswordApi

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
            .baseUrl("https://meokplaylist.store/") // 실제 서버 주소
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ForgotPasswordApi::class.java)

        nextBtn.setOnClickListener {
            handleNext()
        }
    }

    private fun handleNext() {
        val email = emailEdit.text.toString().trim()
        val name = nameEdit.text.toString().trim()
        val birth = birthEdit.text.toString().trim()
        val birthToSend = if (birth.isBlank()) null else birth

        errorMsg.visibility = View.GONE

        if (email.isBlank() || name.isBlank()) {
            showError("이메일과 이름은 필수입니다.")
            return
        }

        val request = ForgotPasswordRequest(email, name, birthToSend)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.findPassword(request)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    } else {
                        showError("정보가 일치하지 않습니다.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("오류 발생: ${e.message}")
                }
            }
        }
    }

    private fun showError(msg: String) {
        errorMsg.text = msg
        errorMsg.visibility = View.VISIBLE
    }
}
