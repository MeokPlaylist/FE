package com.meokpli.app.main.Favorite

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.common.util.Utility
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextStyle
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
    private var currentLabel: com.kakao.vectormap.label.Label? = null

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

                if (lastCameraPosition != null) {
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition))
                } else {
                    val center = LatLng.from(37.28355, 127.04372)
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(center))
                    map.moveCamera(CameraUpdateFactory.zoomTo(4))
                }


                // POI 클릭 시 → 풍선 띄우기
                map.setOnPoiClickListener { _, position, name, layerId ->
                    Log.d("POI", "POI 클릭: name=$name, layerId=$layerId at $position")
                    addCustomLabel(position.latitude, position.longitude, name)
                    sendToBackend(name, position.latitude, position.longitude)
                }

                // 지도 빈 곳 클릭 시 → 풍선 제거
                map.setOnMapClickListener { _, _, _, _ ->
                    balloonContainer.removeAllViews()
                    currentBalloonLatLng = null
                    balloonContainer.visibility = View.GONE

                    currentLabel?.remove()
                    currentLabel = null
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

        // 항상 wrap_content 고정
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        balloonView.layoutParams = params

        // 텍스트 채우기
        balloonView.findViewById<TextView>(R.id.place_name).text = place.place_name
        balloonView.findViewById<TextView>(R.id.place_road_address).text =
            "(도로명) ${place.road_address_name ?: "-"}"

        balloonView.findViewById<TextView>(R.id.place_address).text =
            "(지번) ${place.address_name ?: "-"}"

        val phoneView = balloonView.findViewById<TextView>(R.id.place_phone)
        phoneView.text = place.phone ?: "-"

        // 전화번호가 있으면 클릭 시 다이얼러 실행
        place.phone?.let { phone ->
            if (phone.isNotBlank()) {
                phoneView.paint.isUnderlineText = true

                phoneView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    startActivity(intent)
                }
            }
        }

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
        val offsetY = 100
        // margin 대신 translation 사용
        balloonView.translationX = (pt.x - w / 2).toFloat()
        balloonView.translationY = (pt.y - h - offsetY).toFloat()
    }

    private fun vectorToBitmap(@DrawableRes resId: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(requireContext(), resId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun addCustomLabel(lat: Double, lng: Double, text: String) {
        val bitmap = vectorToBitmap(R.drawable.ic_default_pin)

        val labelLayer = kakaoMap?.labelManager?.layer ?: return

        val style = LabelStyle.from(bitmap)
            .setTextStyles(LabelTextStyle.from(40, Color.BLACK))

        val options = LabelOptions.from(LatLng.from(lat, lng))
            .setStyles(style)

        // 기존 마커 제거
        currentLabel?.remove()

        // 새 마커 추가 & 저장
        currentLabel = labelLayer.addLabel(options)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        kakaoMap?.let {
            lastCameraPosition = it.cameraPosition
        }
        kakaoMap = null
        currentBalloonLatLng = null
    }
    companion object {
        var lastCameraPosition: com.kakao.vectormap.camera.CameraPosition? = null
    }


}
