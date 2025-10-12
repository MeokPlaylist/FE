package com.meokpli.app.main.Home

object CategoryLabels {
    // mood
    private val mood = mapOf(
        "TRADITIONAL" to "전통적인",
        "UNIQUE" to "이색적인",
        "EMOTIONAL" to "감성적인",
        "HEALING" to "힐링되는",
        "GOODVIEW" to "뷰 맛집",
        "ACTIVITY" to "활기찬",
        "LOCAL" to "로컬",
    )
    // food
    private val food = mapOf(
        "BUNSIK" to "분식",
        "CAFE_DESSERT" to "카페/디저트",
        "CHICKEN" to "치킨",
        "CHINESE" to "중식",
        "KOREAN" to "한식",
        "PORK_SASHIMI" to "돈까스/회",
        "FASTFOOD" to "패스트푸드",
        "JOKBAL_BOSSAM" to "족발/보쌈",
        "PIZZA" to "피자",
        "WESTERN" to "양식",
        "MEAT" to "고기",
        "ASIAN" to "아시안",
        "DOSIRAK" to "도시락",
        "LATE_NIGHT" to "야식",
        "JJIM_TANG" to "찜/탕",
    )
    // companion
    private val companion = mapOf(
        "ALONE" to "혼밥",
        "FRIEND" to "친구",
        "COUPLE" to "연인",
        "FAMILY" to "가족",
        "GROUP" to "단체",
        "WITH_PET" to "반려동물 동반",
        "ALUMNI" to "동호회",
    )

    fun toKorean(token: String): String =
        mood[token] ?: food[token] ?: companion[token] ?: token


    private val provinceKo2En = mapOf(
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

    // 2) 한글 → 영문 (시/군/구) — 도/광역시별로 나눔 (동명이인 중구/남구 등 오류 방지)
    //    필요 지역을 계속 보강하세요. (예시는 서울/경기/인천 일부)
    private val cityKo2EnByProvince: Map<String, Map<String, String>> = mapOf(
        "서울" to mapOf(
            "강남구" to "Gangnam-gu", "강동구" to "Gangdong-gu", "강북구" to "Gangbuk-gu",
            "강서구" to "Gangseo-gu", "관악구" to "Gwanak-gu", "광진구" to "Gwangjin-gu",
            "구로구" to "Guro-gu", "금천구" to "Geumcheon-gu", "노원구" to "Nowon-gu",
            "도봉구" to "Dobong-gu", "동대문구" to "Dongdaemun-gu", "동작구" to "Dongjak-gu",
            "마포구" to "Mapo-gu", "서대문구" to "Seodaemun-gu", "서초구" to "Seocho-gu",
            "성동구" to "Seongdong-gu", "성북구" to "Seongbuk-gu", "송파구" to "Songpa-gu",
            "양천구" to "Yangcheon-gu", "영등포구" to "Yeongdeungpo-gu", "용산구" to "Yongsan-gu",
            "은평구" to "Eunpyeong-gu", "종로구" to "Jongno-gu", "중구" to "Jung-gu",
            "중랑구" to "Jungnang-gu"
        ),
        "경기" to mapOf(
            "가평군" to "Gapyeong-gun", "고양시" to "Goyang-si", "과천시" to "Gwacheon-si",
            "광명시" to "Gwangmyeong-si", "광주시" to "Gwangju-si", "구리시" to "Guri-si",
            "군포시" to "Gunpo-si", "김포시" to "Gimpo-si", "남양주시" to "Namyangju-si",
            "동두천시" to "Dongducheon-si", "부천시" to "Bucheon-si", "성남시" to "Seongnam-si",
            "수원시" to "Suwon-si", "시흥시" to "Siheung-si", "안산시" to "Ansan-si",
            "안성시" to "Anseong-si", "안양시" to "Anyang-si", "양주시" to "Yangju-si",
            "양평군" to "Yangpyeong-gun", "여주시" to "Yeoju-si", "연천군" to "Yeoncheon-gun",
            "오산시" to "Osan-si", "용인시" to "Yongin-si", "의왕시" to "Uiwang-si",
            "의정부시" to "Uijeongbu-si", "이천시" to "Icheon-si", "파주시" to "Paju-si",
            "포천시" to "Pocheon-si", "평택시" to "Pyeongtaek-si", "하남시" to "Hanam-si",
            "화성시" to "Hwaseong-si"
        ),
        "인천" to mapOf(
            "강화군" to "Ganghwa-gun", "계양구" to "Gyeyang-gu", "남동구" to "Namdong-gu",
            "동구" to "Dong-gu", "미추홀구" to "Michuhol-gu", "부평구" to "Bupyeong-gu",
            "서구" to "Seo-gu", "연수구" to "Yeonsu-gu", "옹진군" to "Ongjin-gun",
            "중구" to "Jung-gu"
        ),
        "강원" to mapOf(
            "강릉시" to "Gangneung-si", "고성군" to "Goseong-gun", "동해시" to "Donghae-si",
            "삼척시" to "Samcheok-si", "속초시" to "Sokcho-si", "양구군" to "Yanggu-gun",
            "양양군" to "Yangyang-gun", "영월군" to "Yeongwol-gun", "원주시" to "Wonju-si",
            "인제군" to "Inje-gun", "정선군" to "Jeongseon-gun", "춘천시" to "Chuncheon-si",
            "철원군" to "Cheorwon-gun", "태백시" to "Taebaek-si", "평창군" to "Pyeongchang-gun",
            "횡성군" to "Hoengseong-gun", "홍천군" to "Hongcheon-gun", "화천군" to "Hwacheon-gun"
        ),
        "대전" to mapOf(
            "대덕구" to "Daedeok-gu", "동구" to "Dong-gu", "서구" to "Seo-gu",
            "유성구" to "Yuseong-gu", "중구" to "Jung-gu"
        ),
        "세종" to mapOf(
            "세종시" to "Sejong-si"
        ),
        "충남" to mapOf(
            "계룡시" to "Gyeryong-si", "공주시" to "Gongju-si", "금산군" to "Geumsan-gun",
            "논산시" to "Nonsan-si", "당진시" to "Dangjin-si", "보령시" to "Boryeong-si",
            "부여군" to "Buyeo-gun", "서산시" to "Seosan-si", "서천군" to "Seocheon-gun",
            "아산시" to "Asan-si", "예산군" to "Yesan-gun", "천안시" to "Cheonan-si",
            "청양군" to "Cheongyang-gun", "태안군" to "Taean-gun", "홍성군" to "Hongseong-gun"
        ),
        "충북" to mapOf(
            "괴산군" to "Goesan-gun", "단양군" to "Danyang-gun", "보은군" to "Boeun-gun",
            "영동군" to "Yeongdong-gun", "옥천군" to "Okcheon-gun", "음성군" to "Eumseong-gun",
            "증평군" to "Jeungpyeong-gun", "진천군" to "Jincheon-gun", "제천시" to "Jecheon-si",
            "청주시" to "Cheongju-si", "충주시" to "Chungju-si"
        ),
        "부산" to mapOf(
            "강서구" to "Gangseo-gu", "금정구" to "Geumjeong-gu", "기장군" to "Gijang-gun",
            "남구" to "Nam-gu", "동구" to "Dong-gu", "동래구" to "Dongnae-gu",
            "부산진구" to "Busanjin-gu", "북구" to "Buk-gu", "사상구" to "Sasang-gu",
            "사하구" to "Saha-gu", "서구" to "Seo-gu", "수영구" to "Suyeong-gu",
            "영도구" to "Yeongdo-gu", "연제구" to "Yeonje-gu", "중구" to "Jung-gu",
            "해운대구" to "Haeundae-gu"
        ),
        "울산" to mapOf(
            "남구" to "Nam-gu", "동구" to "Dong-gu", "북구" to "Buk-gu",
            "울주군" to "Ulju-gun", "중구" to "Jung-gu"
        ),
        "경남" to mapOf(
            "거제시" to "Geoje-si", "거창군" to "Geochang-gun", "고성군" to "Goseong-gun",
            "김해시" to "Gimhae-si", "남해군" to "Namhae-gun", "밀양시" to "Miryang-si",
            "사천시" to "Sacheon-si", "산청군" to "Sancheong-gun", "양산시" to "Yangsan-si",
            "의령군" to "Uiryeong-gun", "진주시" to "Jinju-si", "창녕군" to "Changnyeong-gun",
            "창원시" to "Changwon-si", "통영시" to "Tongyeong-si", "하동군" to "Hadong-gun",
            "함안군" to "Haman-gun", "함양군" to "Hamyang-gun", "합천군" to "Hapcheon-gun"
        ),
        "경북" to mapOf(
            "경산시" to "Gyeongsan-si", "경주시" to "Gyeongju-si", "고령군" to "Goryeong-gun",
            "구미시" to "Gumi-si", "김천시" to "Gimcheon-si", "문경시" to "Mungyeong-si",
            "봉화군" to "Bonghwa-gun", "상주시" to "Sangju-si", "성주군" to "Seongju-gun",
            "안동시" to "Andong-si", "영덕군" to "Yeongdeok-gun", "영양군" to "Yeongyang-gun",
            "영주시" to "Yeongju-si", "영천시" to "Yeongcheon-si", "예천군" to "Yecheon-gun",
            "울릉군" to "Ulleung-gun", "울진군" to "Uljin-gun", "의성군" to "Uiseong-gun",
            "청도군" to "Cheongdo-gun", "청송군" to "Cheongsong-gun", "칠곡군" to "Chilgok-gun",
            "포항시" to "Pohang-si"
        ),
        "대구" to mapOf(
            "군위군" to "Gunwi-gun", "남구" to "Nam-gu", "달서구" to "Dalseo-gu",
            "달성군" to "Dalseong-gun", "동구" to "Dong-gu", "북구" to "Buk-gu",
            "서구" to "Seo-gu", "수성구" to "Suseong-gu", "중구" to "Jung-gu"
        ),
        "광주" to mapOf(
            "광산구" to "Gwangsan-gu", "남구" to "Nam-gu", "동구" to "Dong-gu",
            "북구" to "Buk-gu", "서구" to "Seo-gu"
        ),
        "전남" to mapOf(
            "강진군" to "Gangjin-gun", "고흥군" to "Goheung-gun", "곡성군" to "Gokseong-gun",
            "광양시" to "Gwangyang-si", "구례군" to "Gurye-gun", "나주시" to "Naju-si",
            "담양군" to "Damyang-gun", "목포시" to "Mokpo-si", "무안군" to "Muan-gun",
            "보성군" to "Boseong-gun", "순천시" to "Suncheon-si", "신안군" to "Shinan-gun",
            "여수시" to "Yeosu-si", "영광군" to "Yeonggwang-gun", "영암군" to "Yeongam-gun",
            "완도군" to "Wando-gun", "장성군" to "Jangseong-gun", "장흥군" to "Jangheung-gun",
            "진도군" to "Jindo-gun", "함평군" to "Hampyeong-gun", "해남군" to "Haenam-gun",
            "화순군" to "Hwasun-gun"
        ),
        "전북" to mapOf(
            "고창군" to "Gochang-gun", "군산시" to "Gunsan-si", "김제시" to "Gimje-si",
            "남원시" to "Namwon-si", "무주군" to "Muju-gun", "부안군" to "Buan-gun",
            "순창군" to "Sunchang-gun", "완주군" to "Wanju-gun", "익산시" to "Iksan-si",
            "임실군" to "Imsil-gun", "장수군" to "Jangsu-gun", "전주시" to "Jeonju-si",
            "정읍시" to "Jeongeup-si", "진안군" to "Jinan-gun"
        ),
        "제주" to mapOf(
            "서귀포시" to "Seogwipo-si", "제주시" to "Jeju-si"
        )

    )

    // 3) 역방향(영문 → 한글) 맵은 자동 생성 (중복 코딩 방지)
    private val provinceEn2Ko: Map<String, String> =
        provinceKo2En.entries.associate { it.value to it.key }

    private val cityEn2KoByProvince: Map<String, Map<String, String>> =
        cityKo2EnByProvince.mapValues { (_, cityMapKo2En) ->
            cityMapKo2En.entries.associate { it.value to it.key }
        }

    /** "Seoul:Gangseo-gu" → "서울:강서구" */
    fun regionToKorean(region: String): String {
        val parts = region.split(":")
        if (parts.size != 2) return region // 형식이 다르면 원문 반환
        val (provEn, cityEn) = parts[0].trim() to parts[1].trim()

        val provKo = provinceEn2Ko[provEn] ?: provEn
        val cityKo = cityEn2KoByProvince[provKo]?.get(cityEn) ?: cityEn
        return "$provKo:$cityKo"
    }

    /** "서울","강서구" → "Seoul:Gangseo-gu" (서버 전송용) */
    fun regionToServer(provinceKo: String, cityKo: String): String {
        val provEn = provinceKo2En[provinceKo] ?: provinceKo
        val cityEn = cityKo2EnByProvince[provinceKo]?.get(cityKo) ?: cityKo
        return "$provEn:$cityEn"
    }
}

