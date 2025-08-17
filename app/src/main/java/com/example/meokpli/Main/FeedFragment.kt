package com.example.meokpli.Main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.meokpli.R
import com.example.meokpli.gallery.GalleryActivity
import android.app.Activity.RESULT_OK
import com.google.android.material.appbar.MaterialToolbar

class FeedFragment : Fragment() {

    private val launchGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK && r.data != null) {
            val uris = r.data!!.getParcelableArrayListExtra<Uri>(GalleryActivity.EXTRA_RESULT_URIS) ?: arrayListOf()
            Toast.makeText(requireContext(), "선택: ${uris.size}장", Toast.LENGTH_SHORT).show()
            // TODO: 다음 단계에서 ViewPager/RecyclerView에 보여주기
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) 툴바 메뉴(업로드) → 갤러리 열기
        view.findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload -> {
                        openGallery()
                        true
                    }
                    else -> false
                }
            }
            setNavigationOnClickListener {
                // 필요하면 뒤로가기 동작 연결
                Toast.makeText(requireContext(), "뒤로가기", Toast.LENGTH_SHORT).show()
            }
        }

        // 2) 다른 버튼에서도 열고 싶으면 이렇게
//        view.findViewById<View>(R.id.btn_category)?.setOnClickListener { openGallery() }
//        view.findViewById<View>(R.id.btn_region)?.setOnClickListener { /* 지역 선택 다이얼로그 등 */ }
    }

    private fun openGallery() {
        val intent = Intent(requireContext(), GalleryActivity::class.java)
        launchGallery.launch(intent)
    }
}
