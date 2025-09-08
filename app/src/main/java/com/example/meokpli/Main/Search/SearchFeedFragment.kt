package com.example.meokpli.Main.Search

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.meokpli.R

class SearchFeedFragment : Fragment(R.layout.activity_category) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val submitButton = view.findViewById<Button>(R.id.submitButton)

        submitButton.setOnClickListener {
            val selectedCategories = collectSelectedCategories()

            // TODO: Retrofit으로 서버에 저장 API 호출
            saveCategoriesToServer(selectedCategories) {
                // 저장 완료 후 → FeedListFragment로 이동
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, FeedListFragment())
                    .commit()
            }
        }
    }

    private fun collectSelectedCategories(): List<String> {
        // ChipGroup 들에서 선택된 값 모아오기
        return listOf("mood:힐링", "food:한식", "companion:친구")
    }

    private fun saveCategoriesToServer(categories: List<String>, onSuccess: () -> Unit) {
        // Retrofit 호출 → 성공 시 onSuccess()
        onSuccess()
    }
}
