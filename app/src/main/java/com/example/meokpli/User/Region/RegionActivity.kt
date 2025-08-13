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
        const val EXTRA_SELECTED_REGIONS = "EXTRA_SELECTED_REGIONS" // 예: ["서울|강남구","경기|고양시"]
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

    // 예시 데이터: 필요시 확장 가능
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

    private var currentSido: String? = "경기" // 초기 선택(디자인 예시 반영)
    private val selectedSigungu = mutableSetOf<String>()   // 현재 시/도에서 선택된 시군구 이름들
    private val selectedPairs = linkedSetOf<String>()      // 전체 선택 결과: "시도|시군구" 포맷
    private val MAX_SELECT = 10

    // 검색용 내부 리스트
    private var fullSigungu = listOf<String>()
    private var filteredSigungu = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_region)

        searchRegion = findViewById(R.id.searchRegion)
        btnCancel = findViewById(R.id.btnCancel)
        btnComplete = findViewById(R.id.btnComplete)
        textSelectedCount = findViewById(R.id.textSelectedCount)
        chipGroupSelected = findViewById(R.id.chipGroupSelected)
        resetArea = findViewById(R.id.resetArea)

        // 시/도
        sidoAdapter = SidoAdapter(
            items = sidoList,
            selected = currentSido
        ) { sido ->
            onSidoChanged(sido)
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerSido).apply {
            layoutManager = LinearLayoutManager(this@RegionActivity)
            adapter = sidoAdapter
        }

        // 시/군/구
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

        // 초기 시군구 로드
        currentSido?.let { onSidoChanged(it) }

        // 검색: 시군구 필터
        searchRegion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSigungu(s?.toString().orEmpty())
            }
        })

        // 초기화
        resetArea.setOnClickListener {
            selectedSigungu.clear()
            selectedPairs.clear()
            syncSelectedChips()
            sigunguAdapter.notifyDataSetChanged()
        }
        btnSearch = findViewById(R.id.btnSearch)
        searchRegion.addTextChangedListener { text ->
            filterSigungu(text.toString())
        }

        // 돋보기(오른쪽 drawable) 터치 시 검색 실행 + 키보드 숨김
        btnSearch.setOnClickListener {
            it.performClick() // 접근성 이벤트
            filterSigungu(searchRegion.text.toString())
            hideKeyboard()
        }

        // 취소: 그냥 종료
        btnCancel.setOnClickListener { finish() }

        // 완료: 선택 결과를 CategoryActivity로 돌려준다
        btnComplete.setOnClickListener {
            val result = Intent().apply {
                putStringArrayListExtra(EXTRA_SELECTED_REGIONS, ArrayList(selectedPairs))
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun onSidoChanged(sido: String) {
        currentSido = sido
        // 다른 시/도로 바꾸면, 그 시/도에 속한 항목들만 리스트로 보여준다
        fullSigungu = sigunguMap[sido].orEmpty()
        filteredSigungu = fullSigungu
        searchRegion.setText("") // 검색 초기화
        // 시/도 바꿨다고 해서 이전 시군구 선택을 강제 삭제하지는 않음(사용자가 여러 시/도에 걸쳐 선택 가능하도록)
        sigunguAdapter.submit(filteredSigungu)
    }

    private fun filterSigungu(query: String) {
        filteredSigungu = if (query.isBlank()) {
            fullSigungu
        } else {
            fullSigungu.filter { it.contains(query.trim(), ignoreCase = true) }
        }
        sigunguAdapter.submit(filteredSigungu)
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun syncSelectedChips() {
        // 하단 ChipGroup 업데이트
        chipGroupSelected.removeAllViews()
        selectedPairs.forEach { pair ->
            val (sido, sgg) = pair.split("|", ignoreCase = false, limit = 2)
            val chip = Chip(this).apply {
                text = "$sgg"
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    // 뱃지에서 제거 시, 리스트 선택도 해제
                    selectedPairs.remove(pair)
                    if (currentSido == sido) {
                        selectedSigungu.remove(sgg)
                        sigunguAdapter.notifyDataSetChanged()
                    } else {
                        // 다른 시/도에 속한 선택은 내부 집합만 제거(시/도 전환 시 화면에서 반영)
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
