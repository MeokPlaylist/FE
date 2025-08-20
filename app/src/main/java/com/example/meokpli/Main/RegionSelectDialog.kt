package com.example.meokpli.Main

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RegionSelectDialog : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "region_result"
        const val KEY_SELECTED_CODES = "selected_codes"     // 결과: ["서울|강남구", ...]
        private const val KEY_PRESELECTED_CODES = "preselected_codes"

        fun newInstance(preselected: ArrayList<String> = arrayListOf()) = RegionSelectDialog().apply {
            arguments = bundleOf(KEY_PRESELECTED_CODES to preselected)
        }
    }

    private lateinit var recyclerSido: RecyclerView
    private lateinit var recyclerSigungu: RecyclerView
    private lateinit var chipGroupSelected: ChipGroup
    private lateinit var textSelectedCount: TextView
    private lateinit var searchRegion: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnCancel: View
    private lateinit var btnComplete: View
    private lateinit var resetArea: View

    private val MAX_SELECT = 10

    // 데이터
    private val sidoList = listOf("서울", "경기", "인천", "강원", "대전", "세종", "충남", "충북", "부산", "울산", "경남", "경북", "대구", "광주", "전남", "전북", "제주")
    private val sigunguMap = mapOf(
        "서울" to listOf("강남구","강동구","강북구","강서구","관악구","광진구","구로구","금천구","노원구","도봉구","동대문구","동작구","마포구","서대문구","서초구","성동구","성북구","송파구","양천구","영등포구","용산구","은평구","종로구","중구","중랑구"),
        "경기" to listOf("가평군","고양시","과천시","광명시","광주시","구리시","군포시","김포시","남양주시","동두천시","부천시","성남시","수원시","시흥시","안산시","안성시","안양시","양주시","양평군","여주시","연천군","오산시","용인시","의왕시","의정부시","이천시","파주시","포천시","평택시","하남시","화성시"),
        "인천" to listOf("강화군","계양구","남동구","동구","미추홀구","부평구","서구","연수구","옹진군","중구"),
        "강원" to listOf("강릉시","고성군","동해시","삼척시","속초시","양구군","양양군","영월군","원주시","인제군","정선군","춘천시","철원군","태백시","평창군","횡성군","홍천군","화천군"),
        "대전" to listOf("대덕구","동구","서구","유성구","중구"),
        "세종" to listOf("세종시"),
        "충남" to listOf("계룡시","공주시","금산군","논산시","당진시","보령시","부여군","서산시","서천군","아산시","예산군","천안시","청양군","태안군","홍성군"),
        "충북" to listOf("괴산군","단양군","보은군","영동군","옥천군","음성군","증평군","진천군","제천시","청주시","충주시"),
        "부산" to listOf("강서구","금정구","기장군","남구","동구","동래구","부산진구","북구","사상구","사하구","서구","수영구","영도구","연제구","중구","해운대구"),
        "울산" to listOf("남구","동구","북구","울주군","중구"),
        "경남" to listOf("거제시","거창군","고성군","김해시","남해군","밀양시","사천시","산청군","양산시","의령군","진주시","창녕군","창원시","통영시","하동군","함안군","함양군","합천군"),
        "경북" to listOf("경산시","경주시","고령군","구미시","김천시","문경시","봉화군","상주시","성주군","안동시","영덕군","영양군","영주시","영천시","예천군","울릉군","울진군","의성군","청도군","청송군","칠곡군","포항시"),
        "대구" to listOf("군위군","남구","달서구","달성군","동구","북구","서구","수성구","중구"),
        "광주" to listOf("광산구","남구","동구","북구","서구"),
        "전남" to listOf("강진군","고흥군","곡성군","광양시","구례군","나주시","담양군","목포시","무안군","보성군","순천시","신안군","여수시","영광군","영암군","완도군","장성군","장흥군","진도군","함평군","해남군","화순군"),
        "전북" to listOf("고창군","군산시","김제시","남원시","무주군","부안군","순창군","완주군","익산시","임실군","장수군","전주시","정읍시","진안군"),
        "제주" to listOf("서귀포시","제주시")
    )

    private var currentSido: String = "서울"
    private val selectedPairs = linkedSetOf<String>()   // 전체 선택: "시도|시군구"
    private val selectedSigunguInCurrent = linkedSetOf<String>() // 현재 시/도 내 선택

    // 목록 데이터(현재 시/도 + 검색)
    private var fullSigungu = listOf<String>()
    private var filteredSigungu = listOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_region_select, null, false)

        // 기존 선택 주입
        val pre = arguments?.getStringArrayList(KEY_PRESELECTED_CODES) ?: arrayListOf()
        selectedPairs.clear()
        selectedPairs.addAll(pre)

        // findView
        recyclerSido = v.findViewById(R.id.recyclerSido)
        recyclerSigungu = v.findViewById(R.id.recyclerSigungu)
        chipGroupSelected = v.findViewById(R.id.chipGroupSelected)
        textSelectedCount = v.findViewById(R.id.textSelectedCount)
        searchRegion = v.findViewById(R.id.searchRegion)
        btnSearch = v.findViewById(R.id.btnSearch)
        btnCancel = v.findViewById(R.id.btnCancel)
        btnComplete = v.findViewById(R.id.btnComplete)
        resetArea = v.findViewById(R.id.resetArea)

        // 시/도 리스트
        recyclerSido.layoutManager = LinearLayoutManager(requireContext())
        recyclerSido.adapter = SidoAdapter(
            items = sidoList,
            selected = currentSido
        ) { sido ->
            onSidoChanged(sido)
        }

        // 시/군/구 리스트
        recyclerSigungu.layoutManager = LinearLayoutManager(requireContext())
        recyclerSigungu.adapter = SigunguAdapter(
            items = listOf(),
            selected = selectedSigunguInCurrent,
            maxCount = MAX_SELECT,
            onToggle = { name, added ->
                val code = "$currentSido|$name"
                if (added) {
                    if (selectedPairs.size >= MAX_SELECT) {
                        Toast.makeText(requireContext(), "최대 $MAX_SELECT 개까지 선택할 수 있어요.", Toast.LENGTH_SHORT).show()
                        return@SigunguAdapter
                    }
                    selectedPairs.add(code)
                } else {
                    selectedPairs.remove(code)
                }
                // 현재 시/도 내 선택 셋 유지
                if (added) selectedSigunguInCurrent.add(name) else selectedSigunguInCurrent.remove(name)
                (recyclerSigungu.adapter as SigunguAdapter).notifyDataSetChanged()
                syncSelectedChips()
            }
        )

        // 초기 시/도 로드
        onSidoChanged(currentSido)

        // 칩 초기 렌더
        syncSelectedChips()

        // 검색
        searchRegion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSigungu(s?.toString().orEmpty())
            }
        })
        btnSearch.setOnClickListener { filterSigungu(searchRegion.text.toString()) }

        // 초기화
        resetArea.setOnClickListener {
            selectedPairs.clear()
            selectedSigunguInCurrent.clear()
            (recyclerSigungu.adapter as SigunguAdapter).notifyDataSetChanged()
            syncSelectedChips()
        }

        // 취소/완료
        btnCancel.setOnClickListener { dismiss() }
        btnComplete.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(KEY_SELECTED_CODES to ArrayList(selectedPairs))
            )
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.App_MdcAlertDialog)
            .setView(v)
            .create()
    }

    private fun onSidoChanged(sido: String) {
        currentSido = sido

        // 현재 시/도 내 선택 복원
        val inThis = selectedPairs.filter { it.startsWith("$sido|") }.map { it.substringAfter("|") }
        selectedSigunguInCurrent.clear()
        selectedSigunguInCurrent.addAll(inThis)

        // 목록
        fullSigungu = sigunguMap[sido].orEmpty()
        filteredSigungu = fullSigungu
        searchRegion.setText("")
        (recyclerSigungu.adapter as SigunguAdapter).submit(filteredSigungu)
        (recyclerSido.adapter as SidoAdapter).setSelected(sido)
    }

    private fun filterSigungu(query: String) {
        filteredSigungu = if (query.isBlank()) fullSigungu
        else fullSigungu.filter { it.contains(query.trim(), ignoreCase = true) }
        (recyclerSigungu.adapter as SigunguAdapter).submit(filteredSigungu)
    }

    private fun syncSelectedChips() {
        chipGroupSelected.removeAllViews()
        selectedPairs.forEach { code ->
            val (sido, sgg) = code.split("|", limit = 2)
            val chip = Chip(requireContext()).apply {
                text = sgg
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedPairs.remove(code)
                    if (currentSido == sido) {
                        selectedSigunguInCurrent.remove(sgg)
                        (recyclerSigungu.adapter as SigunguAdapter).notifyDataSetChanged()
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

    // --- 어댑터들(간단 버전, row를 코드로 생성) ---

    private class SidoAdapter(
        private val items: List<String>,
        private var selected: String,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SidoAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val text = v.findViewById<TextView>(R.id.textRegion)
            val indicator = v.findViewById<View>(R.id.leftIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_region_sido, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val isSelected = (item == selected)

            holder.text.text = item
            // 선택 스타일: 왼쪽 인디케이터 + 연한 배경
            holder.indicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.itemView.setBackgroundColor(
                if (isSelected) 0xFFFFF3EF.toInt() else 0x00000000
            )

            holder.itemView.setOnClickListener {
                if (selected != item) {
                    val old = selected
                    selected = item
                    notifyItemChanged(items.indexOf(old))
                    notifyItemChanged(position)
                    onClick(item)
                }
            }
        }

        fun setSelected(s: String) {
            if (selected == s) return
            val old = selected
            selected = s
            notifyItemChanged(items.indexOf(old))
            notifyItemChanged(items.indexOf(selected))
        }
    }

    // ----- SIGUNGU -----
    private class SigunguAdapter(
        private var items: List<String>,
        private val selected: MutableSet<String>,
        private val maxCount: Int,
        private val onToggle: (name: String, added: Boolean) -> Unit
    ) : RecyclerView.Adapter<SigunguAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val text = v.findViewById<TextView>(R.id.textRegion)
            val check = v.findViewById<TextView>(R.id.checkView) // "✓"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_region_sigungu, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val name = items[position]
            val isChecked = selected.contains(name)

            holder.text.text = name
            // 선택 시 텍스트 빨강, 체크 ✓ 오른쪽 표시
            holder.text.setTextColor(
                if (isChecked) 0xFFC64132.toInt() else 0xFF000000.toInt()
            )
            holder.check.visibility = if (isChecked) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                if (isChecked) {
                    selected.remove(name)
                    onToggle(name, false)
                    notifyItemChanged(position)
                } else {
                    if (selected.size >= maxCount) {
                        // onToggle 내부에서 토스트 처리
                        onToggle(name, true)
                    } else {
                        selected.add(name)
                        onToggle(name, true)
                        notifyItemChanged(position)
                    }
                }
            }
        }

        fun submit(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}

// px 변환
private fun dp(v: Int) = (v * (android.content.res.Resources.getSystem().displayMetrics.density)).toInt()