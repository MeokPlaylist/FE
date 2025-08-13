package com.example.meokpli.Main

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

class FeedFragment : Fragment() {

    private val launchGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == requireActivity().RESULT_OK && r.data != null) {
            val uris = r.data!!.getParcelableArrayListExtra<Uri>(GalleryActivity.EXTRA_RESULT_URIS) ?: arrayListOf()
            Toast.makeText(requireContext(), "선택: ${uris.size}장", Toast.LENGTH_SHORT).show()
            // TODO: 다음 단계에서 ViewPager/RecyclerView에 보여주기
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_add_photos).setOnClickListener {
            val intent = Intent(requireContext(), GalleryActivity::class.java)
            launchGallery.launch(intent)
        }
    }
}
