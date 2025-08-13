package com.example.meokpli.gallery

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.google.android.material.appbar.MaterialToolbar

class GalleryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT_URIS = "result_uris"
        const val MAX_SELECT = 10
    }

    private lateinit var adapter: GalleryAdapter
    private val allUris = mutableListOf<Uri>()
    private val selected = mutableListOf<Uri>()

    private val requestPerms = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
            val ok = granted.values.all { it }
        if (ok) loadImages() else {
            Toast.makeText(this, "사진 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
                val tvCancel = findViewById<TextView>(R.id.tv_cancel)
                val tvDone = findViewById<TextView>(R.id.tv_done)
                val rv = findViewById<RecyclerView>(R.id.rv)

                toolbar.setNavigationOnClickListener { finish() }
        tvCancel.setOnClickListener { finish() }
        tvDone.setOnClickListener {
            setResult(RESULT_OK, Intent().putParcelableArrayListExtra(EXTRA_RESULT_URIS, ArrayList(selected)))
            finish()
        }

        rv.layoutManager = GridLayoutManager(this, 3)
        adapter = GalleryAdapter(
                items = allUris,
                isSelected = { uri -> selected.contains(uri) },
                orderOf = { uri -> selected.indexOf(uri).takeIf { it >= 0 }?.plus(1) },
        onToggle = { uri ->
        if (selected.contains(uri)) selected.remove(uri)
        else {
            if (selected.size >= MAX_SELECT) {
                Toast.makeText(this, "최대 ${MAX_SELECT}장까지 선택할 수 있어요", Toast.LENGTH_SHORT).show()
                return@GalleryAdapter
            }
            selected.add(uri)
        }
        // 간단하게 전체 갱신(성능 충분)
        adapter.notifyDataSetChanged()

        // 선택 1개 이상이면 '추가' 버튼 진하게
        tvDone.alpha = if (selected.isNotEmpty()) 1f else 0.6f
            }
        )
        rv.adapter = adapter

        ensurePermissionThenLoad()
    }

    private fun ensurePermissionThenLoad() {
        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        requestPerms.launch(perms)
    }

    private fun loadImages() {
        allUris.clear()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
        )?.use { c ->
                val idxId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idxId)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                allUris.add(uri)
            }
        }
        adapter.notifyDataSetChanged()
    }
}
