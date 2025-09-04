package com.example.meokpli.Main.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.R

class MyFeedThumbnailAdapter(
    private val items: MutableList<MyPageItem> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_YEAR_HEADER = 0 //public으로 변경
        const val TYPE_PHOTO = 1
        const val TYPE_REGION_ROW = 2
    }
    //프래그먼트에서 상수볼라도 되도록 헬퍼 추가
    fun isHeaderPosition(position: Int): Boolean =
        items.getOrNull(position) is MyPageItem.YearHeader

    inner class YearHeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvHeader: TextView = v.findViewById(R.id.tvYear)
    }

    inner class PhotoVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivThumbnail)
    }

    inner class RegionRowVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvRegion: TextView = v.findViewById(R.id.tvRegion)
        val rvPhotos: RecyclerView = v.findViewById(R.id.rvPhotos)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is MyPageItem.YearHeader -> TYPE_YEAR_HEADER
            is MyPageItem.Photo -> TYPE_PHOTO
            is MyPageItem.RegionRow -> TYPE_REGION_ROW
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_YEAR_HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_year_header, parent, false)
                YearHeaderVH(v)
            }
            TYPE_PHOTO -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_my_feed_thumbnail, parent, false)
                PhotoVH(v)
            }
            TYPE_REGION_ROW -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_region_row, parent, false)
                RegionRowVH(v)
            }
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MyPageItem.YearHeader -> (holder as YearHeaderVH).tvHeader.text = item.year.toString()

            is MyPageItem.Photo -> {
                val vh = holder as PhotoVH
                vh.ivThumb.load(item.url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                }
            }

            is MyPageItem.RegionRow -> {
                val vh = holder as RegionRowVH
                vh.tvRegion.text = translateRegion(item.region)
                vh.rvPhotos.layoutManager =
                    LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                vh.rvPhotos.adapter = PhotoAdapter(item.photoUrls, isRegionMode = true)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MyPageItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun translateRegion(region: String): String {
        val parts = region.split(":")
        return if (parts.size == 2) {
            val province = provinceReverseMap[parts[0]] ?: parts[0]
            val city = cityReverseMap[parts[1]] ?: parts[1]
            "$province $city"
        } else {
            provinceReverseMap[region] ?: cityReverseMap[region] ?: region
        }
    }
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
    private val provinceReverseMap: Map<String, String> =
        provinceMap.entries.associate { (kor, eng) -> eng to kor }

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
    private val cityReverseMap: Map<String, String> =
        cityMap.entries.associate { (kor, eng) -> eng to kor }
}
