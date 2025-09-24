package com.meokpli.app.user.Region

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.meokpli.app.R
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

    private val sidoList = listOf("서울", "경기", "인천", "강원", "대전", "세종", "충남", "충북", "부산", "울산", "경남", "경북", "대구", "광주", "전남", "전북", "제주")
    private val sigunguMap = mapOf(
        "서울" to listOf("강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"),
        "경기" to listOf("가평군", "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시", "성남시", "수원시", "시흥시", "안산시", "안성시", "안양시", "양주시", "양평군", "여주시", "연천군", "오산시", "용인시", "의왕시", "의정부시", "이천시", "파주시", "포천시", "평택시", "하남시", "화성시"),
        "인천" to listOf("강화군", "계양구", "남동구", "동구", "미추홀구", "부평구", "서구", "연수구", "옹진군", "중구"),
        "강원" to listOf("강릉시", "고성군", "동해시", "삼척시", "속초시", "양구군", "양양군", "영월군", "원주시", "인제군", "정선군", "춘천시", "철원군", "태백시", "평창군", "횡성군", "홍천군", "화천군"),
        "대전" to listOf("대덕구", "동구", "서구", "유성구", "중구"),
        "세종" to listOf("세종시"),
        "충남" to listOf("계룡시", "공주시", "금산군", "논산시", "당진시", "보령시", "부여군", "서산시", "서천군", "아산시", "예산군", "천안시", "청양군", "태안군", "홍성군"),
        "충북" to listOf("괴산군", "단양군", "보은군", "영동군", "옥천군", "음성군", "증평군", "진천군", "제천시", "청주시", "충주시"),
        "부산" to listOf("강서구", "금정구", "기장군", "남구", "동구", "동래구", "부산진구", "북구", "사상구", "사하구", "서구", "수영구", "영도구", "연제구", "중구", "해운대구"),
        "울산" to listOf("남구", "동구", "북구", "울주군", "중구"),
        "경남" to listOf("거제시", "거창군", "고성군", "김해시", "남해군", "밀양시", "사천시", "산청군", "양산시", "의령군", "진주시", "창녕군", "창원시", "통영시", "하동군", "함안군", "함양군", "합천군"),
        "경북" to listOf("경산시", "경주시", "고령군", "구미시", "김천시", "문경시", "봉화군", "상주시", "성주군", "안동시", "영덕군", "영양군", "영주시", "영천시", "예천군", "울릉군", "울진군", "의성군", "청도군", "청송군", "칠곡군", "포항시"),
        "대구" to listOf("군위군", "남구", "달서구", "달성군", "동구","북구", "서구", "수성구", "중구"),
        "광주" to listOf("광산구", "남구", "동구", "북구", "서구"),
        "전남" to listOf("강진군", "고흥군", "곡성군", "광양시", "구례군", "나주시", "담양군", "목포시", "무안군", "보성군", "순천시", "신안군", "여수시", "영광군", "영암군", "완도군", "장성군", "장흥군", "진도군", "함평군", "해남군", "화순군"),
        "전북" to listOf("고창군", "군산시", "김제시", "남원시", "무주군", "부안군", "순창군", "완주군", "익산시", "임실군", "장수군", "전주시", "정읍시", "진안군"),
        "제주" to listOf("서귀포시", "제주시")
    )

    private var currentSido: String? = "경기"
    private val selectedSigungu = mutableSetOf<String>()   // 현재 시/도 내 체크 상태
    private val selectedPairs = linkedSetOf<String>()      // 전체 선택: "시도:시군구"
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
                val pair = "${currentSido ?: ""}:$name"
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
            .filter { it.startsWith("$sido:") }
            .map { it.substringAfter(":") }
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
            val (sido, sgg) = pair.split(":", ignoreCase = false, limit = 2)
            val chip = Chip(this).apply {
                // ✅ "울산 동구" 형태로 표시
                text = "$sido $sgg"

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
