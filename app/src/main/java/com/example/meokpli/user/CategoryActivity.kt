package com.example.meokpli.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.meokpli.auth.Network
import com.example.meokpli.R
import com.example.meokpli.user.Region.RegionActivity
import com.example.meokpli.user.Category.*
import com.example.meokpli.user.Category.CategorySetUpRequest
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CategoryActivity : AppCompatActivity() {

    private lateinit var chipGroupMood: ChipGroup
    private lateinit var chipGroupFood: ChipGroup
    private lateinit var chipGroupCompanion: ChipGroup
    private lateinit var chipGroupRegions: ChipGroup
    // 이미 있는 selectedRegions: List<String>  // 예: ["서울:강남구","경기:고양시"]
    private lateinit var submitButton: Button
    private lateinit var plusRegionButton: Button



    private val moodList = listOf("전통적인","이색적인","감성적인", "힐링되는", "뷰 맛집", "활기찬", "로컬")
    private val foodList = listOf("분식", "카페/디저트", "치킨", "중식", "한식", "돈까스/회","패스트푸드","족발/보쌈","피자","양식","고기","아시안","도시락","야식","찜/탕")
    private val companionList = listOf("혼밥", "친구", "연인", "가족", "단체","반려동물 동반","동호회")
    // 지역은 RegionActivity에서 고른 결과를 받아 저장한다고 가정 (지금은 빈 배열로 보냄)
    private var selectedRegions: List<String> = emptyList()

    private val moodMap = mapOf(
        "전통적인" to "TRADITIONAL",
        "이색적인" to "UNIQUE",
        "감성적인" to "EMOTIONAL",
        "힐링되는" to "HEALING",
        "뷰 맛집" to "GOODVIEW",
        "활기찬" to "ACTIVITY",
        "로컬" to "LOCAL"
    )

    private val foodMap = mapOf(
        "분식" to "BUNSIK",
        "카페/디저트" to "CAFE_DESSERT",
        "치킨" to "CHICKEN",
        "중식" to "CHINESE",
        "한식" to "KOREAN",
        "돈까스/회" to "PORK_SASHIMI",
        "패스트푸드" to "FASTFOOD",
        "족발/보쌈" to "JOKBAL_BOSSAM",
        "피자" to "PIZZA",
        "양식" to "WESTERN",
        "고기" to "MEAT",
        "아시안" to "ASIAN",
        "도시락" to "DOSIRAK",
        "야식" to "LATE_NIGHT",
        "찜/탕" to "JJIM_TANG"
    )

    private val companionMap = mapOf(
        "혼밥" to "ALONE",
        "친구" to "FRIEND",
        "연인" to "COUPLE",
        "가족" to "FAMILY",
        "단체" to "GROUP",
        "반려동물 동반" to "WITH_PET",
        "동호회" to "ALUMNI"
    )
    private val provinceMap = mapOf(
        "서울" to "Seoul",
        "경기" to "Gyeonggi",
        "인천" to "Incheon",
        "강원" to "Gangwon",
        "대전" to "Daejeon",
        "세종" to "Sejong",
        "충남" to "Chungnam",
        "충북" to "Chungbuk",
        "부산" to "Busan",
        "울산" to "Ulsan",
        "경남" to "Gyeongnam",
        "경북" to "Gyeongbuk",
        "대구" to "Daegu",
        "광주" to "Gwangju",
        "전남" to "Jeonnam",
        "전북" to "Jeonbuk",
        "제주" to "Jeju"
    )
    private val cityMap = mapOf(
        // --- Seoul
        "강남구" to "Gangnam-gu",
        "강동구" to "Gangdong-gu",
        "강북구" to "Gangbuk-gu",
        "강서구" to "Gangseo-gu",
        "관악구" to "Gwanak-gu",
        "광진구" to "Gwangjin-gu",
        "구로구" to "Guro-gu",
        "금천구" to "Geumcheon-gu",
        "노원구" to "Nowon-gu",
        "도봉구" to "Dobong-gu",
        "동대문구" to "Dongdaemun-gu",
        "동작구" to "Dongjak-gu",
        "마포구" to "Mapo-gu",
        "서대문구" to "Seodaemun-gu",
        "서초구" to "Seocho-gu",
        "성동구" to "Seongdong-gu",
        "성북구" to "Seongbuk-gu",
        "송파구" to "Songpa-gu",
        "양천구" to "Yangcheon-gu",
        "영등포구" to "Yeongdeungpo-gu",
        "용산구" to "Yongsan-gu",
        "은평구" to "Eunpyeong-gu",
        "종로구" to "Jongno-gu",
        "중구" to "Jung-gu",
        "중랑구" to "Jungnang-gu",

        // --- Gyeonggi
        "가평군" to "Gapyeong-gun",
        "고양시" to "Goyang-si",
        "과천시" to "Gwacheon-si",
        "광명시" to "Gwangmyeong-si",
        "광주시" to "Gwangju-si",
        "구리시" to "Guri-si",
        "군포시" to "Gunpo-si",
        "김포시" to "Gimpo-si",
        "남양주시" to "Namyangju-si",
        "동두천시" to "Dongducheon-si",
        "부천시" to "Bucheon-si",
        "성남시" to "Seongnam-si",
        "수원시" to "Suwon-si",
        "시흥시" to "Siheung-si",
        "안산시" to "Ansan-si",
        "안성시" to "Anseong-si",
        "안양시" to "Anyang-si",
        "양주시" to "Yangju-si",
        "양평군" to "Yangpyeong-gun",
        "여주시" to "Yeoju-si",
        "연천군" to "Yeoncheon-gun",
        "오산시" to "Osan-si",
        "용인시" to "Yongin-si",
        "의왕시" to "Uiwang-si",
        "의정부시" to "Uijeongbu-si",
        "이천시" to "Icheon-si",
        "파주시" to "Paju-si",
        "포천시" to "Pocheon-si",
        "평택시" to "Pyeongtaek-si",
        "하남시" to "Hanam-si",
        "화성시" to "Hwaseong-si",

        // --- Incheon
        "강화군" to "Ganghwa-gun",
        "계양구" to "Gyeyang-gu",
        "남동구" to "Namdong-gu",
        "동구" to "Dong-gu",
        "미추홀구" to "Michuhol-gu",
        "부평구" to "Bupyeong-gu",
        "서구" to "Seo-gu",
        "연수구" to "Yeonsu-gu",
        "옹진군" to "Ongjin-gun",
        "중구(인천)" to "Jung-gu",   // 중복되니 구분 필요하면 키 바꿔

        // --- Gangwon
        "강릉시" to "Gangneung-si",
        "고성군(강원)" to "Goseong-gun",
        "동해시" to "Donghae-si",
        "삼척시" to "Samcheok-si",
        "속초시" to "Sokcho-si",
        "양구군" to "Yanggu-gun",
        "양양군" to "Yangyang-gun",
        "영월군" to "Yeongwol-gun",
        "원주시" to "Wonju-si",
        "인제군" to "Inje-gun",
        "정선군" to "Jeongseon-gun",
        "춘천시" to "Chuncheon-si",
        "철원군" to "Cheorwon-gun",
        "태백시" to "Taebaek-si",
        "평창군" to "Pyeongchang-gun",
        "횡성군" to "Hoengseong-gun",
        "홍천군" to "Hongcheon-gun",
        "화천군" to "Hwacheon-gun",

        // --- Daejeon
        "대덕구" to "Daedeok-gu",
        "동구(대전)" to "Dong-gu",
        "서구(대전)" to "Seo-gu",
        "유성구" to "Yuseong-gu",
        "중구(대전)" to "Jung-gu",

        // --- Sejong
        "세종시" to "Sejong-si",

        // --- Chungnam
        "계룡시" to "Gyeryong-si",
        "공주시" to "Gongju-si",
        "금산군" to "Geumsan-gun",
        "논산시" to "Nonsan-si",
        "당진시" to "Dangjin-si",
        "보령시" to "Boryeong-si",
        "부여군" to "Buyeo-gun",
        "서산시" to "Seosan-si",
        "서천군" to "Seocheon-gun",
        "아산시" to "Asan-si",
        "예산군" to "Yesan-gun",
        "천안시" to "Cheonan-si",
        "청양군" to "Cheongyang-gun",
        "태안군" to "Taean-gun",
        "홍성군" to "Hongseong-gun",

        // --- Chungbuk
        "괴산군" to "Goesan-gun",
        "단양군" to "Danyang-gun",
        "보은군" to "Boeun-gun",
        "영동군" to "Yeongdong-gun",
        "옥천군" to "Okcheon-gun",
        "음성군" to "Eumseong-gun",
        "증평군" to "Jeungpyeong-gun",
        "진천군" to "Jincheon-gun",
        "제천시" to "Jecheon-si",
        "청주시" to "Cheongju-si",
        "충주시" to "Chungju-si",

        // --- Busan
        "강서구(부산)" to "Gangseo-gu",
        "금정구" to "Geumjeong-gu",
        "기장군" to "Gijang-gun",
        "남구(부산)" to "Nam-gu",
        "동구(부산)" to "Dong-gu",
        "동래구" to "Dongnae-gu",
        "부산진구" to "Busanjin-gu",
        "북구(부산)" to "Buk-gu",
        "사상구" to "Sasang-gu",
        "사하구" to "Saha-gu",
        "서구(부산)" to "Seo-gu",
        "수영구" to "Suyeong-gu",
        "영도구" to "Yeongdo-gu",
        "연제구" to "Yeonje-gu",
        "중구(부산)" to "Jung-gu",
        "해운대구" to "Haeundae-gu",

        // --- Ulsan
        "남구(울산)" to "Nam-gu",
        "동구(울산)" to "Dong-gu",
        "북구(울산)" to "Buk-gu",
        "울주군" to "Ulju-gun",
        "중구(울산)" to "Jung-gu",

        // --- Gyeongnam
        "거제시" to "Geoje-si",
        "거창군" to "Geochang-gun",
        "고성군(경남)" to "Goseong-gun",
        "김해시" to "Gimhae-si",
        "남해군" to "Namhae-gun",
        "밀양시" to "Miryang-si",
        "사천시" to "Sacheon-si",
        "산청군" to "Sancheong-gun",
        "양산시" to "Yangsan-si",
        "의령군" to "Uiryeong-gun",
        "진주시" to "Jinju-si",
        "창녕군" to "Changnyeong-gun",
        "창원시" to "Changwon-si",
        "통영시" to "Tongyeong-si",
        "하동군" to "Hadong-gun",
        "함안군" to "Haman-gun",
        "함양군" to "Hamyang-gun",
        "합천군" to "Hapcheon-gun",

        // --- Gyeongbuk
        "경산시" to "Gyeongsan-si",
        "경주시" to "Gyeongju-si",
        "고령군" to "Goryeong-gun",
        "구미시" to "Gumi-si",
        "김천시" to "Gimcheon-si",
        "문경시" to "Mungyeong-si",
        "봉화군" to "Bonghwa-gun",
        "상주시" to "Sangju-si",
        "성주군" to "Seongju-gun",
        "안동시" to "Andong-si",
        "영덕군" to "Yeongdeok-gun",
        "영양군" to "Yeongyang-gun",
        "영주시" to "Yeongju-si",
        "영천시" to "Yeongcheon-si",
        "예천군" to "Yecheon-gun",
        "울릉군" to "Ulleung-gun",
        "울진군" to "Uljin-gun",
        "의성군" to "Uiseong-gun",
        "청도군" to "Cheongdo-gun",
        "청송군" to "Cheongsong-gun",
        "칠곡군" to "Chilgok-gun",
        "포항시" to "Pohang-si",

        // --- Daegu
        "군위군" to "Gunwi-gun",
        "남구(대구)" to "Nam-gu",
        "달서구" to "Dalseo-gu",
        "달성군" to "Dalseong-gun",
        "동구(대구)" to "Dong-gu",
        "북구(대구)" to "Buk-gu",
        "서구(대구)" to "Seo-gu",
        "수성구" to "Suseong-gu",
        "중구(대구)" to "Jung-gu",

        // --- Gwangju
        "광산구" to "Gwangsan-gu",
        "남구(광주)" to "Nam-gu",
        "동구(광주)" to "Dong-gu",
        "북구(광주)" to "Buk-gu",
        "서구(광주)" to "Seo-gu",

        // --- Jeonnam
        "강진군" to "Gangjin-gun",
        "고흥군" to "Goheung-gun",
        "곡성군" to "Gokseong-gun",
        "광양시" to "Gwangyang-si",
        "구례군" to "Gurye-gun",
        "나주시" to "Naju-si",
        "담양군" to "Damyang-gun",
        "목포시" to "Mokpo-si",
        "무안군" to "Muan-gun",
        "보성군" to "Boseong-gun",
        "순천시" to "Suncheon-si",
        "신안군" to "Shinan-gun",
        "여수시" to "Yeosu-si",
        "영광군" to "Yeonggwang-gun",
        "영암군" to "Yeongam-gun",
        "완도군" to "Wando-gun",
        "장성군" to "Jangseong-gun",
        "장흥군" to "Jangheung-gun",
        "진도군" to "Jindo-gun",
        "함평군" to "Hampyeong-gun",
        "해남군" to "Haenam-gun",
        "화순군" to "Hwasun-gun",

        // --- Jeonbuk
        "고창군" to "Gochang-gun",
        "군산시" to "Gunsan-si",
        "김제시" to "Gimje-si",
        "남원시" to "Namwon-si",
        "무주군" to "Muju-gun",
        "부안군" to "Buan-gun",
        "순창군" to "Sunchang-gun",
        "완주군" to "Wanju-gun",
        "익산시" to "Iksan-si",
        "임실군" to "Imsil-gun",
        "장수군" to "Jangsu-gun",
        "전주시" to "Jeonju-si",
        "정읍시" to "Jeongeup-si",
        "진안군" to "Jinan-gun",

        // --- Jeju
        "서귀포시" to "Seogwipo-si",
        "제주시" to "Jeju-si"
    )

    private fun translate(list: List<String>, dict: Map<String, String>): List<String> {
        return list.mapNotNull { dict[it] } // 매핑 없는 값은 버림(null 제외)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        submitButton = findViewById(R.id.submitButton)
        plusRegionButton = findViewById(R.id.PlusRegionButton)

        submitButton.isEnabled = true
        submitButton.isClickable = true

        chipGroupMood = findViewById(R.id.chipGroupMood)
        chipGroupFood = findViewById(R.id.chipGroupFood)
        chipGroupCompanion = findViewById(R.id.chipGroupCompanion)
        chipGroupRegions = findViewById(R.id.chipGroupRegions)

        createChips(chipGroupMood, moodList)
        createChips(chipGroupFood, foodList)
        createChips(chipGroupCompanion, companionList)

        plusRegionButton.setOnClickListener {
            val intent = Intent(this, RegionActivity::class.java).apply {
                putStringArrayListExtra(
                    RegionActivity.EXTRA_PRESELECTED, ArrayList(selectedRegions)
                )
            }
            regionLauncher.launch(intent)
        }

        submitButton.setOnClickListener {
            if (!hasAllSelected()) {
                Toast.makeText(this, "분위기·음식·동반자에서 각각 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selected = getSelectedChips()

            val categories = buildCategoryStrings(
                moodsKo = selected["분위기"] ?: emptyList(),
                foodsKo = selected["음식"] ?: emptyList(),
                companionsKo = selected["동반자"] ?: emptyList(),
                regions = selectedRegions
            )
            val regionsEng = buildRegionsEng(selectedRegions)

            val request = CategorySetUpRequest(
                categories = categories,               // ex) ["moods:GOODVIEW", "foods:CAFEDESERT", "companions:FRIEND"]
                regions = regionsEng                // ex) ["Gangwon:Samcheok-si","Gangwon:Yangyang-gun"]
            )
            Log.d("d",request.toString())
            lifecycleScope.launch {
                val api: CategoryApi = Network.categoryApi(this@CategoryActivity)
                runCatching {
                    withContext(Dispatchers.IO) { api.setupCategories(request) }
                }
                    .onSuccess { res ->
                        // Category API 성공 시 User API 호출
                        val userApi = Network.userApi(this@CategoryActivity)

                        runCatching {
                            withContext(Dispatchers.IO) { userApi.newBCheck() }
                        }
                            .onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@CategoryActivity, "카테고리 설정 완료!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@CategoryActivity, WelcomeActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .onFailure { e ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@CategoryActivity, "UserApi 호출 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .onFailure { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CategoryActivity, "카테고리 설정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
    private fun buildCategoryStrings(
        moodsKo: List<String>,
        foodsKo: List<String>,
        companionsKo: List<String>,
        regions: List<String> // ← 시그니처는 유지하지만, 이 값은 여기서 쓰지 않음
    ): List<String> {
        val result = mutableListOf<String>()
        translate(moodsKo, moodMap).forEach       { result += "moods:$it" }
        translate(foodsKo, foodMap).forEach       { result += "foods:$it" }
        translate(companionsKo, companionMap).forEach { result += "companions:$it" }
        // regions는 여기서 처리하지 않음!
        // (이전의 "regions:..." push 전부 제거)
        return result
    }
    private fun buildRegionsEng(regions: List<String>): List<String> {

        fun mapRegionOne(ko: String): String? {
            // "서울:강남구" 또는 "서울:강남구" 모두 허용
            val parts = ko.split(':', ':')
            if (parts.size != 2) return null

            val provinceKo = parts[0].trim()
            val cityKo     = parts[1].trim()

            val provinceEn = provinceMap[provinceKo] ?: provinceKo

            // 모호한 구명은 "중구(부산)" 같은 키 우선
            val disambiguatedKey = "$cityKo($provinceKo)"
            val cityEn = when {
                cityMap.containsKey(disambiguatedKey) -> cityMap[disambiguatedKey]!!
                cityMap.containsKey(cityKo)           -> cityMap[cityKo]!!
                else                                   -> cityKo // 매핑 없으면 원문 유지
            }

            // 서버 표준 구분자: ':'
            return "$provinceEn:$cityEn"
        }

        return regions.orEmpty().mapNotNull { mapRegionOne(it) }
    }

    private fun hasAllSelected(): Boolean {
        return chipGroupMood.checkedChipIds.isNotEmpty() &&
                chipGroupFood.checkedChipIds.isNotEmpty() &&
                chipGroupCompanion.checkedChipIds.isNotEmpty()
    }

    private fun createChips(chipGroup: ChipGroup, items: List<String>) {
        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isCheckable = true
                isClickable = true
                isCloseIconVisible = false
                chipIcon = null
                checkedIcon = null
                // 배경/텍스트 색 상태 셀렉터
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))

                // 테두리 색/두께 (선택 전 0.75dp 느낌, 선택 후 #C64132)
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                // MaterialChip 스타일 보정
                isCloseIconVisible = false
                isFocusable = false
            }
            chipGroup.addView(chip)
        }
    }

    private fun getSelectedChips(): Map<String, List<String>> {
        fun selectedFrom(group: ChipGroup): List<String> {
            return (0 until group.childCount)
                .mapNotNull { group.getChildAt(it) as? Chip }
                .filter { it.isChecked }
                .map { it.text.toString() }
        }

        return mapOf(
            "분위기" to selectedFrom(chipGroupMood),
            "음식" to selectedFrom(chipGroupFood),
            "동반자" to selectedFrom(chipGroupCompanion)
        )
    }

    private fun renderSelectedRegions() {
        chipGroupRegions.removeAllViews()

        selectedRegions.forEach { regionCode ->
            // 화면 표시는 "서울 강남구"처럼, 서버 전송은 원본 그대로 유지
            val label = regionCode.replace(":", " ")

            val chip = Chip(this).apply {
                text = label
                isCheckable = false
                isClickable = true
                isCloseIconVisible = true         // ⨯ 표시
                chipIcon = null
                checkedIcon = null

                // 기존 칩들과 동일한 스타일
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
                setChipStrokeColorResource(R.color.selector_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density * 0.75f

                // 삭제 핸들러
                setOnCloseIconClickListener {
                    // 리스트에서 제거하고 다시 렌더
                    selectedRegions = selectedRegions.filterNot { it == regionCode }
                    renderSelectedRegions()
                }
            }
            chipGroupRegions.addView(chip)
        }
    }

    // onCreate() 내에 launcher 등록
    private val regionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val list = result.data
                ?.getStringArrayListExtra(RegionActivity.EXTRA_SELECTED_REGIONS)
                ?: return@registerForActivityResult   // null이면 기존 유지

            // 비우기로 확정한 경우만 빈 리스트 수용하고 싶다면 조건 추가 가능
            selectedRegions = list
            renderSelectedRegions()
            Toast.makeText(this, "지역 ${selectedRegions.size}개 선택됨", Toast.LENGTH_SHORT).show()
        }
        // RESULT_CANCELED면 아무 것도 하지 않음 → 기존 선택 유지
    }

}
