package com.meokpli.app.user

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.meokpli.app.R

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms) // 아래 #3 레이아웃

        // 상단 뒤로가기
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // 각 화살표에 클릭 연결
        setArrowClick(findViewById(R.id.arrowTermsService),
            "terms_service.txt", "먹플리 이용약관 동의")
        setArrowClick(findViewById(R.id.arrowPrivacyPolicy),
            "privacy_policy.txt", "개인정보 수집이용 동의")
        setArrowClick(findViewById(R.id.arrowLocationPolicy),
            "location_policy.txt", "위치정보 수집 및 이용 동의")
        setArrowClick(findViewById(R.id.arrowProfilePolicy),
            "profile_policy.txt", "프로필 정보 추가 수집 동의")
        setArrowClick(findViewById(R.id.arrowPhotoPolicy),
            "photo_policy.txt", "사진 위치·시간 정보 수집 및 이용 동의")
    }

    private fun setArrowClick(arrow: ImageView, assetFile: String, title: String) {
        arrow.setOnClickListener {
            val text = loadAssetText("terms/$assetFile") // ★ 경로 보정
            showTermsPopup(title, text)
        }
    }
    private fun loadAssetText(fileName: String): String {
        return assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun showTermsPopup(title: String, content: String) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.popup_terms, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvPopupTitle).text = title
        val tvContent = view.findViewById<TextView>(R.id.tvPopupContent)

        // ✅ HTML 파싱 적용
        tvContent.text = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)

        view.findViewById<ImageView>(R.id.btnPopupClose).setOnClickListener { dialog.dismiss() }

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}
