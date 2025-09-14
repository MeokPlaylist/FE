package com.meokpli.app.user

import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.data.remote.response.PersonalInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AccountInfoActivity : AppCompatActivity() {

    // UI
    private lateinit var tvName: TextView
    private lateinit var tvBirth: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAccountDate: TextView
    private lateinit var tvLoginMethod: TextView

    // API (인증 자동첨부)
    private val userApi by lazy { Network.userApi(this) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_info) // XML 매핑 (valueAccountdate/valueLoginmethod 사용)

        // 뒤로가기
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 값 칸
        tvName = findViewById(R.id.valueName)
        tvBirth = findViewById(R.id.valueBirth)
        tvEmail = findViewById(R.id.valueEmail)
        tvAccountDate = findViewById(R.id.valueAccountdate)     // 계정 생성일  :contentReference[oaicite:1]{index=1}
        tvLoginMethod = findViewById(R.id.valueLoginmethod)     // 소셜 로그인 방식 :contentReference[oaicite:2]{index=2}

        // 서버에서 개인정보 가져오기
        fetchPersonalInfo()
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun bind(info: PersonalInfoResponse) {
        tvName.text = info.name ?: "-"
        tvEmail.text = info.email ?: "-"

        // 생년월일: "yyyy-MM-dd" → "yyyy.MM.dd"
        tvBirth.text = formatDateToDot(info.birthDay)

        // 계정 생성일: OffsetDateTime(예: "2025-08-28T10:20:30+09:00") → "yyyy.MM.dd"
        tvAccountDate.text = formatCreatedAt(info.createdAt)

        // 소셜 로그인 방식: false → "먹플리계정", true → "소셜계정"
        val isSocial = info.OauthUser == true
        tvLoginMethod.text = if (isSocial) "소셜 계정" else "먹플리 계정"
    }

    private fun formatDateToDot(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        // "yyyy-MM-dd" 또는 "yyyy-MM-ddTHH:mm..." 모두 앞 10자리만 사용
        val head10 = if (raw.length >= 10) raw.substring(0, 10) else raw
        return head10.replace('-', '.')
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatCreatedAt(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        // 우선 ISO-8601로 파싱 시도, 실패하면 앞 10자리로 대체
        return try {
            val odt = OffsetDateTime.parse(raw)
            odt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        } catch (_: Exception) {
            formatDateToDot(raw)
        }
    }
}

