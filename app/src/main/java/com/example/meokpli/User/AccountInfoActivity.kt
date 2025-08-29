package com.example.meokpli.User

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.R
import com.example.meokpli.Auth.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountInfoActivity : AppCompatActivity() {

    // UI
    private lateinit var tvName: TextView
    private lateinit var tvBirth: TextView
    private lateinit var tvEmail: TextView

    // API (인증 자동첨부)
    private val userApi by lazy { Network.userApi(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_info) // XML과 매핑 :contentReference[oaicite:0]{index=0}

        // 뒤로가기
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 값 칸
        tvName = findViewById(R.id.valueName)
        tvBirth = findViewById(R.id.valueBirth)
        tvEmail = findViewById(R.id.valueEmail)

        // 서버에서 개인정보 가져오기
        fetchPersonalInfo()
    }

    private fun fetchPersonalInfo() {
        lifecycleScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    userApi.getPersonalInfo() // GET /user/personalInfor
                }
                bind(info)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AccountInfoActivity, "개인정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //이건 교체 필요할듯 저장 현식에 따라

    private fun bind(info: PersonalInfoResponse) {
        tvName.text = info.name ?: "-"
        tvEmail.text = info.email ?: "-"

        // birthDay 포매팅: "yyyy-MM-dd" → "yyyy.MM.dd"
        tvBirth.text = formatDate(info.birthDay)
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        // 예: "2025-08-28T10:20:30Z" → 앞 10자리 "2025-08-28"만 사용
        val isoDate = (if (raw.length >= 10) raw.substring(0, 10) else raw).replace('-', '.')
        return isoDate // "2025.08.28"
    }
}
