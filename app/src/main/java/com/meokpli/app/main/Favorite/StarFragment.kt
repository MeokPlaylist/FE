package com.meokpli.app.main.Favorite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.data.remote.request.SearchPlaceRequest
import com.meokpli.app.data.remote.response.SearchPlaceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarFragment : Fragment() {

    private var kakaoMap: KakaoMap? = null
    private lateinit var mapView: MapView
    private lateinit var balloonContainer: FrameLayout

    private lateinit var placeApi: PlaceApi

    // 풍선 좌표 저장
    private var currentBalloonLatLng: LatLng? = null

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

        placeApi = Network.placeApi(requireContext())

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

                // POI 클릭 시 → 풍선 띄우기
                map.setOnPoiClickListener { _, position, name, layerId ->
                    Log.d("POI", "POI 클릭: name=$name, layerId=$layerId at $position")
                    sendToBackend(name, position.latitude, position.longitude)
                }

                // 지도 빈 곳 클릭 시 → 풍선 제거
                map.setOnMapClickListener { _, _, _, _ ->
                    balloonContainer.removeAllViews()
                    currentBalloonLatLng = null
                }

                // 카메라 이동 시작 → 풍선 숨김
                map.setOnCameraMoveStartListener(object : KakaoMap.OnCameraMoveStartListener {
                    override fun onCameraMoveStart(kakaoMap: KakaoMap, gestureType: GestureType) {
                        if (balloonContainer.childCount > 0) {
                            balloonContainer.visibility = View.GONE
                        }
                    }
                })

                // 카메라 이동 끝 → 풍선 다시 보이기 + 위치 보정
                map.setOnCameraMoveEndListener(object : KakaoMap.OnCameraMoveEndListener {
                    override fun onCameraMoveEnd(
                        kakaoMap: KakaoMap,
                        position: com.kakao.vectormap.camera.CameraPosition,
                        gestureType: GestureType
                    ) {
                        currentBalloonLatLng?.let {
                            updateBalloonPosition(it)
                            balloonContainer.visibility = View.VISIBLE
                        }
                    }
                })
            }
        })
    }

    /**
     * 좌표 + placeId를 서버로 보내서 상세 정보 조회
     */
    private fun sendToBackend(name: String, lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val request = SearchPlaceRequest(lat = lat, lng = lng)
                Log.d("StarFragment", "request:$request")
                val response: SearchPlaceResponse = withContext(Dispatchers.IO) {
                    placeApi.searchPlace(request)
                }
                Log.d("StarFragment", "백엔드 응답: $response")

                showBalloon(LatLng.from(lat, lng), response)

            } catch (e: Exception) {
                Log.e("StarFragment", "백엔드 호출 실패", e)
            }
        }
    }

    /**
     * 풍선 표시 (항상 하나만 유지)
     */
    private fun showBalloon(position: LatLng, place: SearchPlaceResponse) {
        balloonContainer.removeAllViews()
        currentBalloonLatLng = position

        val balloonView = layoutInflater.inflate(R.layout.custom_balloon, balloonContainer, false)

        balloonView.findViewById<TextView>(R.id.place_name).text = place.place_name
        balloonView.findViewById<TextView>(R.id.place_road_address).text =
            place.road_address_name ?: "-"
        balloonView.findViewById<TextView>(R.id.place_address).text =
            place.address_name ?: "-"
        balloonView.findViewById<TextView>(R.id.place_phone).text =
            place.phone ?: "-"

        balloonView.findViewById<TextView>(R.id.tv_detail).setOnClickListener {
            if (!place.place_url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(place.place_url))
                startActivity(intent)
            }
        }

        balloonContainer.addView(balloonView)
        updateBalloonPosition(position) // 초기 위치 반영
        balloonContainer.visibility = View.VISIBLE
    }

    /**
     * 풍선 위치 갱신
     */
    private fun updateBalloonPosition(latLng: LatLng) {
        val pt = kakaoMap?.toScreenPoint(latLng) ?: return
        val balloonView = balloonContainer.getChildAt(0) ?: return

        balloonView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val w = balloonView.measuredWidth
        val h = balloonView.measuredHeight

        val params = balloonView.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = pt.x - w / 2
        params.topMargin = pt.y - h
        balloonView.layoutParams = params
    }

    override fun onDestroyView() {
        super.onDestroyView()
        kakaoMap = null
        currentBalloonLatLng = null
    }
}
