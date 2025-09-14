package com.meokpli.app.main.Favorite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.JsonParser
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.meokpli.app.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import kotlin.concurrent.thread

class StarFragment : Fragment() {

    private var kakaoMap: KakaoMap? = null
    private lateinit var mapView: MapView
    private lateinit var balloonContainer: FrameLayout
    private val client = OkHttpClient()

    // 카카오 REST API 키 (반드시 "KakaoAK " 접두어 포함)
    private val restApiKey = "d8fd3cc299e7921ad9cbce305123c7a8"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_star, container, false)
        mapView = root.findViewById(R.id.map_view)
        balloonContainer = root.findViewById(R.id.balloon_container)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception?) {
                Log.e("StarFragment", "지도 로딩 실패: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map

                val center = LatLng.from(37.28355, 127.04372)
                map.moveCamera(CameraUpdateFactory.newCenterPosition(center))
                map.moveCamera(CameraUpdateFactory.zoomTo(4))

                // 👉 기본 POI 클릭 이벤트
                map.setOnPoiClickListener { kakaoMap, position, name, layerId ->
                    balloonContainer.removeAllViews()
                    Log.d("POI", "클릭: $name, 좌표: $position, layer=$layerId")

                    fetchPlaceDetail(name, position.latitude, position.longitude) { placeDto ->
                        requireActivity().runOnUiThread {
                            if (placeDto != null) {
                                showBalloon(position, placeDto)
                            }
                        }
                    }
                }
            }
        })
    }

    // Kakao Local API로 상세 정보 요청
    private fun fetchPlaceDetail(
        name: String, lat: Double, lng: Double,
        callback: (KakaoPlaceDto?) -> Unit
    ) {
        thread {
            try {
                val query = URLEncoder.encode(name, "UTF-8")
                val url =
                    "https://dapi.kakao.com/v2/local/search/keyword.json" +
                            "?query=$query&x=$lng&y=$lat&radius=100&sort=distance"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "KakaoAK $restApiKey")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("StarFragment", "HTTP 응답: code=${response.code}, body=${response.body}")

                    if (!response.isSuccessful) {
                        callback(null)
                        return@use
                    }

                    val body = response.body?.string()
                    if (body == null) {
                        callback(null)
                        return@use
                    }

                    val json = JsonParser.parseString(body).asJsonObject
                    val documents = json.getAsJsonArray("documents")

                    if (documents.size() > 0) {
                        val obj = documents[0].asJsonObject
                        val place = KakaoPlaceDto(
                            id = obj["id"].asString,
                            name = obj["place_name"].asString,
                            category = obj["category_group_name"]?.asString,
                            phone = obj["phone"]?.asString,
                            roadAddress = obj["road_address_name"]?.asString,
                            jibunAddress = obj["address_name"]?.asString,
                            lat = obj["y"].asString.toDouble(),
                            lng = obj["x"].asString.toDouble(),
                            placeUrl = obj["place_url"].asString
                        )
                        callback(place)
                    } else {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("StarFragment", "API 호출 에러", e)
                callback(null)
            }
        }
    }

    // 풍선 뷰 표시
    private fun showBalloon(position: LatLng, place: KakaoPlaceDto) {
        balloonContainer.removeAllViews()

        val balloonView = layoutInflater.inflate(R.layout.custom_balloon, balloonContainer, false)

        balloonView.findViewById<TextView>(R.id.place_name).text = place.name
        balloonView.findViewById<TextView>(R.id.place_address).text =
            place.roadAddress ?: place.jibunAddress ?: "-"
        balloonView.findViewById<TextView>(R.id.place_phone).text =
            place.phone ?: "-"

        balloonView.findViewById<TextView>(R.id.tv_detail).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(place.placeUrl))
            startActivity(intent)
        }

        val favBtn = balloonView.findViewById<ImageView>(R.id.btn_favorite)
        favBtn.tag = false
        favBtn.setOnClickListener {
            val isChecked = favBtn.tag as Boolean
            if (isChecked) {
                favBtn.setImageResource(R.drawable.ic_star_unchecked)
                favBtn.tag = false
                removeFavorite(place)
            } else {
                favBtn.setImageResource(R.drawable.ic_star_filled)
                favBtn.tag = true
                saveFavorite(place)
            }
        }

        // 좌표 -> 스크린 좌표
        val pt = kakaoMap?.toScreenPoint(position) ?: return

        // 뷰 크기 미리 측정
        balloonView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val w = balloonView.measuredWidth
        val h = balloonView.measuredHeight

        // 마커 위 중앙에 위치하도록 보정
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = pt.x - w / 2
            topMargin = pt.y - h
        }

        balloonContainer.addView(balloonView, params)
    }


    // 찜 저장 (DB/서버 연동 자리)
    private fun saveFavorite(place: KakaoPlaceDto) {
        Log.d("Favorite", "찜 저장: ${place.name}")
        // TODO: Room DB insert or 서버 API 호출
    }
    private fun removeFavorite(place: KakaoPlaceDto){
        Log.d("Favorite", "찜삭제: ${place.name}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        kakaoMap = null
    }
}

// DTO
data class KakaoPlaceDto(
    val id: String,
    val name: String,
    val category: String?,
    val phone: String?,
    val roadAddress: String?,
    val jibunAddress: String?,
    val lat: Double,
    val lng: Double,
    val placeUrl: String
)
