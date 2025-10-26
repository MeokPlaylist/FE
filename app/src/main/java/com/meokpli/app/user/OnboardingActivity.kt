package com.meokpli.app.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meokpli.app.main.MainActivity
import com.meokpli.app.R
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private var step = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        //상태바 표시
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
        showStep(1)
    }

    private fun showStep(target: Int) {
        step = target
        when (step) {
            1 -> {
                setContentView(R.layout.activity_onboarding_step1)
                findViewById<MaterialButton>(R.id.buttonNext).setOnClickListener {
                    showStep(2)
                }
            }
            2 -> {
                setContentView(R.layout.activity_onboarding_step2)
                findViewById<MaterialButton>(R.id.buttonNext).setOnClickListener {
                    showStep(3)
                }
            }
            3 -> {
                setContentView(R.layout.activity_onboarding_step3)
                findViewById<MaterialButton>(R.id.buttonNext).setOnClickListener {
                    showStep(4)
                }
            }
            4 -> {
                setContentView(R.layout.activity_onboarding_step4)
                findViewById<MaterialButton>(R.id.buttonDone).setOnClickListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
