package com.example.meokpli.User.Region

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meokpli.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import android.content.Context
import android.view.View

class RegionActivity : AppCompatActivity() {

    companion object {
        // CategoryActivity와 결과 주고받는 키
        const val EXTRA_SELECTED_REGIONS = "EXTRA_SELECTED_REGIONS" // 결과용
        const val EXTRA_PRESELECTED = "EXTRA_PRESELECTED"           // 진입 시 미리 선택값
    }

    private lateinit var searchRegion: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnComplete: Button
    private lateinit var textSelectedCount: TextView
    private lateinit var chipGroupSelected: ChipGroup
    private lateinit var resetArea: View
    private lateinit var btnSearch: ImageButton

    private lateinit var sidoAdapter: SidoAdapter
    private lateinit var sigunguAdapter: SigunguAdapter

    private val sidoList = listOf("서울", "경기", "강원도", "경상남도", "경상북도", "부산", "대구", "대전", "광주")
    private val sigunguMap = mapOf(
        "서울" to listOf("강남구","서초구","송파구","강동구","마포구","용산구","종로구","성동구"),
        "경기" to listOf("고양시","광주시","군포시","남양주시","가평군","김포시","구리시","광명시"),
        "강원도" to listOf("춘천시","원주시","강릉시"),
        "경상남도" to listOf("창원시","김해시","진주시"),
        "경상북도" to listOf("포항시","경주시","구미시"),
        "부산" to listOf("해운대구","수영구","남구","동래구"),
        "대구" to listOf("수성구","달서구","중구"),
        "대전" to listOf("유성구","서구","중구"),
        "광주" to listOf("북구","서구","동구")
    )

    private var currentSido: String? = "경기"
    private val selectedSigungu = mutableSetOf<String>()   // 현재 시/도 내 체크 상태
    private val selectedPairs = linkedSetOf<String>()      // 전체 선택: "시도|시군구"
    private val MAX_SELECT = 10

    private var fullSigungu = listOf<String>()
    private var filteredSigungu = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_region)

        // 1) 진입 시 기존 선택 반영
        val preselected = intent.getStringArrayListExtra(EXTRA_PRESELECTED) ?: arrayListOf()
        selectedPairs.clear()
        selectedPairs.addAll(preselected)

        searchRegion = findViewById(R.id.searchRegion)
        btnCancel = findViewById(R.id.btnCancel)
        btnComplete = findViewById(R.id.btnComplete)
        textSelectedCount = findViewById(R.id.textSelectedCount)
        chipGroupSelected = findViewById(R.id.chipGroupSelected)
        resetArea = findViewById(R.id.resetArea)
        btnSearch = findViewById(R.id.btnSearch)

        // 시/도 리스트
        sidoAdapter = SidoAdapter(
            items = sidoList,
            selected = currentSido
        ) { sido -> onSidoChanged(sido) }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerSido).apply {
            layoutManager = LinearLayoutManager(this@RegionActivity)
            adapter = sidoAdapter
        }

        // 시/군/구 리스트
        sigunguAdapter = SigunguAdapter(
            items = listOf(),
            selected = selectedSigungu,
            maxCount = MAX_SELECT,
            onToggle = { name, added ->
                val pair = "${currentSido ?: ""}|$name"
                if (added) {
                    selectedPairs.add(pair)
                } else {
                    selectedPairs.remove(pair)
                }
                syncSelectedChips()
            },
            onLimitExceeded = {
                Toast.makeText(this, "최대 $MAX_SELECT 개까지 선택할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
        )

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerSigungu).apply {
            layoutManager = LinearLayoutManager(this@RegionActivity)
            adapter = sigunguAdapter
        }

        // 2) 현재 시/도 기준으로 체크 상태 주입 + 목록 로드
        currentSido?.let { onSidoChanged(it) }

        // 3) 하단 칩 초기 렌더
        syncSelectedChips()

        // 검색
        searchRegion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSigungu(s?.toString().orEmpty())
            }
        })
        searchRegion.addTextChangedListener { text -> filterSigungu(text.toString()) }
        btnSearch.setOnClickListener {
            filterSigungu(searchRegion.text.toString())
            hideKeyboard()
        }

        // 초기화
        resetArea.setOnClickListener {
            selectedSigungu.clear()
            selectedPairs.clear()
            syncSelectedChips()
            sigunguAdapter.notifyDataSetChanged()
        }

        // 완료: 현재 선택 반환
        btnComplete.setOnClickListener {
            val selected = ArrayList(selectedPairs) // ✅ 전체 선택을 그대로 반환
            setResult(RESULT_OK, Intent().apply {
                putStringArrayListExtra(EXTRA_SELECTED_REGIONS, selected)
            })
            finish()
        }

        // 취소: 기존 값 유지
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun onSidoChanged(sido: String) {
        currentSido = sido

        // 현재 시/도에 해당하는 선택만 selectedSigungu로 복사(참조 유지 위해 clear/addAll)
        val fromPairs = selectedPairs
            .filter { it.startsWith("$sido|") }
            .map { it.substringAfter("|") }
        selectedSigungu.clear()
        selectedSigungu.addAll(fromPairs)

        // 목록 갱신
        fullSigungu = sigunguMap[sido].orEmpty()
        filteredSigungu = fullSigungu
        searchRegion.setText("")
        sigunguAdapter.submit(filteredSigungu)   // 어댑터가 selectedSigungu를 참조해 체크 상태 표시
    }

    private fun filterSigungu(query: String) {
        filteredSigungu = if (query.isBlank()) fullSigungu
        else fullSigungu.filter { it.contains(query.trim(), ignoreCase = true) }
        sigunguAdapter.submit(filteredSigungu)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun syncSelectedChips() {
        chipGroupSelected.removeAllViews()
        selectedPairs.forEach { pair ->
            val (sido, sgg) = pair.split("|", ignoreCase = false, limit = 2)
            val chip = Chip(this).apply {
                text = sgg
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedPairs.remove(pair)
                    if (currentSido == sido) {
                        selectedSigungu.remove(sgg)
                        sigunguAdapter.notifyDataSetChanged()
                    }
                    syncSelectedChips()
                }
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(resources.getColorStateList(R.color.selector_chip_text, null))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f
            }
            chipGroupSelected.addView(chip)
        }
        textSelectedCount.text = "${selectedPairs.size} / $MAX_SELECT"
    }
}
