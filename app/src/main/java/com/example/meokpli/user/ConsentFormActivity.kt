package com.example.meokpli.user

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.auth.Network
import com.example.meokpli.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsentFormActivity : AppCompatActivity() {

    private lateinit var btnConfirm: Button
    private lateinit var cbAllAgree: CheckBox
    private lateinit var requiredCheckBoxes: List<CheckBox>
    private lateinit var userApi: UserApi
    private var backPressedTime: Long = 0
    private val BACK_PRESS_INTERVAL = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent_form)
        //JWT가 자동으로 헤더에 실림
        userApi = Network.userApi(this)

        onBackPressedDispatcher.addCallback(this) {
            // 홈 화면에서 종료 시 두 번 눌러야 꺼지도록
            if (System.currentTimeMillis() - backPressedTime < BACK_PRESS_INTERVAL) {
                finish()
            } else {
                Toast.makeText(this@ConsentFormActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                backPressedTime = System.currentTimeMillis()


            }
        }

        btnConfirm = findViewById(R.id.btnConfirm)
        cbAllAgree = findViewById(R.id.cbAllAgree)
        btnConfirm.setOnClickListener {
            val agreed = cbAllAgree.isChecked
            if (!agreed) {
                Toast.makeText(this, "모든 약관에 동의해야 진행할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val req = ConsentAgreeRequest(isAvailable = true)
            btnConfirm.isEnabled = false
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) {
                        userApi.consentAgree(req)

                    }
                    if (res.isAvailable) {
                        Toast.makeText(this@ConsentFormActivity, "약관 동의가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ConsentFormActivity, InitProfileActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@ConsentFormActivity, res.message ?: "실패했습니다.", Toast.LENGTH_SHORT).show()
                        btnConfirm.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ConsentFormActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnConfirm.isEnabled = true
                }
            }
        }


        requiredCheckBoxes = listOf(
            findViewById(R.id.cbAgePolicy),
            findViewById(R.id.cbTermsService),
            findViewById(R.id.cbPrivacyPolicy),
            findViewById(R.id.cbLocationPolicy),
            findViewById(R.id.cbProfilePolicy),
            findViewById(R.id.cbPhotoPolicy)
        )


            // 전체 동의 클릭 시 모든 체크박스 상태 변경
        cbAllAgree.setOnCheckedChangeListener { _, isChecked ->
            requiredCheckBoxes.forEach { it.setOnCheckedChangeListener(null) }
            requiredCheckBoxes.forEach { it.isChecked = isChecked }
            requiredCheckBoxes.forEach { cb ->
                cb.setOnCheckedChangeListener { _, _ ->
                    syncAllAgreeCheckbox()
                    updateConfirmButton()
                }
            }
            updateConfirmButton()
        }

        // 개별 항목 체크 시 전체 동의 체크박스 상태 업데이트
        requiredCheckBoxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                syncAllAgreeCheckbox()
                updateConfirmButton()
            }
        }

        // 다음 버튼 초기 상태 비활성화
        updateConfirmButton()

        // Arrow 클릭 시 팝업 띄우기
        setArrowClick(findViewById(R.id.arrowAgeService), "age_policy.txt", "만 14세 이상입니다.")
        setArrowClick(findViewById(R.id.arrowTermsService), "terms_service.txt", "먹플리 이용약관 동의")
        setArrowClick(findViewById(R.id.arrowPrivacyPolicy), "privacy_policy.txt", "개인정보 수집이용 동의")
        setArrowClick(findViewById(R.id.arrowLocationPolicy), "location_policy.txt", "위치정보 수집 및 이용 동의")
        setArrowClick(findViewById(R.id.arrowProfilePolicy), "profile_policy.txt", "프로필 정보 추가 수집 동의")
        setArrowClick(findViewById(R.id.arrowPhotoPolicy), "photo_policy.txt", "사진 위치 및 시간 정보 수집 및 이용 동의")
    }

    private fun syncAllAgreeCheckbox() {
        val allChecked = requiredCheckBoxes.all { it.isChecked }
        cbAllAgree.setOnCheckedChangeListener(null)
        cbAllAgree.isChecked = allChecked
        cbAllAgree.setOnCheckedChangeListener { _, isChecked ->
            requiredCheckBoxes.forEach { it.setOnCheckedChangeListener(null) }
            requiredCheckBoxes.forEach { it.isChecked = isChecked }
            requiredCheckBoxes.forEach { cb ->
                cb.setOnCheckedChangeListener { _, _ ->
                    syncAllAgreeCheckbox()
                    updateConfirmButton()
                }
            }
            updateConfirmButton()
        }
    }

    private fun updateConfirmButton() {
        val allChecked = requiredCheckBoxes.all { it.isChecked }
        btnConfirm.isEnabled = allChecked
        btnConfirm.setBackgroundColor(if (allChecked) 0xFFC64132.toInt() else 0xFFEEEEEE.toInt())
        btnConfirm.setTextColor(if (allChecked) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
    }

    private fun setArrowClick(arrow: ImageView, fileName: String, title: String) {
        arrow.setOnClickListener {
            val content = loadTextFromAsset("terms/$fileName")
            showTermsPopup(title, content)
        }
    }

    private fun loadTextFromAsset(filePath: String): String {
        val inputStream = assets.open(filePath)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val content = StringBuilder()
        bufferedReader.forEachLine { line ->
            content.append(line).append("\n")
        }
        return content.toString()
    }

    private fun showTermsPopup(title: String, content: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_terms)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvPopupTitle)
        val tvContent = dialog.findViewById<TextView>(R.id.tvPopupContent)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnPopupClose)

        tvTitle.text = title
        tvContent.text = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
