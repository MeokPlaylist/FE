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
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.meokpli.app.R
import com.meokpli.app.auth.Network
import com.meokpli.app.data.remote.request.RemoveFavoriteRequest
import com.meokpli.app.data.remote.request.SaveFavoriteRequest
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

    private var currentBalloonLatLng: LatLng? = null
    private var currentLabel: Label? = null

    // 좌표를 key로 favorite 라벨 저장
    private val favoriteLabels = mutableMapOf<Pair<Double, Double>, Label>()

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

                // 진입 시 즐겨찾기 불러오기
                lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            placeApi.getFavorite()
                        }
                        response.placeCoordinates.forEach { coord ->
                            addFavoriteLabel(coord.x, coord.y)
                        }
                    } catch (e: Exception) {
                        Log.e("StarFragment", "getFavorite 호출 실패", e)
                    }
                }

                // 다른 가게는 기존 POI 클릭 로직 그대로 유지
                map.setOnPoiClickListener { _, position, name, layerId ->
                    Log.d("POI", "POI 클릭: name=$name, layerId=$layerId at $position")
                    addCustomLabel(position.latitude, position.longitude, name)
                    sendToBackend(name, position.latitude, position.longitude, false)
                }

                // 별마커 클릭 처리
                map.setOnLabelClickListener(object : KakaoMap.OnLabelClickListener {
                    override fun onLabelClicked(
                        kakaoMap: KakaoMap,
                        layer: LabelLayer,
                        label: Label
                    ) {
                        if (label.tag == "FAVORITE") {
                            val pos = label.position
                            currentLabel?.remove()
                            currentLabel = null
                            sendToBackend("Favorite", pos.latitude, pos.longitude, true)
                        }
                    }
                })

                // 지도 빈 곳 클릭 시 → 풍선 제거
                map.setOnMapClickListener { _, _, _, _ ->
                    balloonContainer.removeAllViews()
                    currentBalloonLatLng = null
                    balloonContainer.visibility = View.GONE
                    currentLabel?.remove()
                    currentLabel = null
                }

                // 카메라 이동 시작/끝에 대한 풍선 처리
                map.setOnCameraMoveStartListener { _, _ ->
                    if (balloonContainer.childCount > 0) {
                        balloonContainer.visibility = View.GONE
                    }
                }
                map.setOnCameraMoveEndListener { _, _, _ ->
                    currentBalloonLatLng?.let {
                        updateBalloonPosition(it)
                        balloonContainer.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    // ------------------------ 풍선 처리 ------------------------

    private fun sendToBackend(name: String, lat: Double, lng: Double, fromFavorite: Boolean = false) {
        lifecycleScope.launch {
            try {
                val request = SearchPlaceRequest(lat = lat, lng = lng)
                val response: SearchPlaceResponse = withContext(Dispatchers.IO) {
                    placeApi.searchPlace(request)
                }
                showBalloon(LatLng.from(lat, lng), response, fromFavorite)
            } catch (e: Exception) {
                Log.e("StarFragment", "백엔드 호출 실패", e)
            }
        }
    }

    private fun showBalloon(position: LatLng, place: SearchPlaceResponse, fromFavorite: Boolean) {
        balloonContainer.removeAllViews()
        currentBalloonLatLng = position
        Log.d("Position", "${position.latitude},${position.longitude}")

        val balloonView = layoutInflater.inflate(R.layout.custom_balloon, balloonContainer, false)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        balloonView.layoutParams = params

        balloonView.findViewById<TextView>(R.id.place_name).text = place.place_name
        balloonView.findViewById<TextView>(R.id.place_road_address).text =
            "(도로명) ${place.road_address_name ?: "-"}"
        balloonView.findViewById<TextView>(R.id.place_address).text =
            "(지번) ${place.address_name ?: "-"}"

        val phoneView = balloonView.findViewById<TextView>(R.id.place_phone)
        phoneView.text = place.phone ?: "-"
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

        // 찜 버튼 처리
        val favBtn = balloonView.findViewById<ImageView>(R.id.btn_favorite)

        var isFavorite = fromFavorite
        updateFavoriteIcon(favBtn, isFavorite)

        favBtn.setOnClickListener {
            currentLabel?.remove()
            currentLabel = null
            lifecycleScope.launch {
                try {
                    if (isFavorite) {
                        // 즐겨찾기 제거
                        withContext(Dispatchers.IO) {
                            placeApi.removeFavorite(
                                RemoveFavoriteRequest(
                                    lat = position.latitude,
                                    lng = position.longitude
                                )
                            )
                        }
                        // 지도에서 라벨 제거 & 리스트에서 제거
                        favoriteLabels.remove(position.latitude to position.longitude)?.remove()

                        isFavorite = false
                        Log.d("StarFragment", "즐겨찾기 제거 완료")
                    } else {
                        // 즐겨찾기 저장
                        withContext(Dispatchers.IO) {
                            placeApi.saveFavorite(
                                SaveFavoriteRequest(
                                    lat = position.latitude,
                                    lng = position.longitude
                                )
                            )
                        }
                        isFavorite = true
                        addFavoriteLabel(position.latitude, position.longitude)
                        Log.d("StarFragment", "즐겨찾기 저장 완료")
                    }
                    updateFavoriteIcon(favBtn, isFavorite)
                } catch (e: Exception) {
                    Log.e("StarFragment", "즐겨찾기 토글 실패", e)
                }
            }
        }

        balloonContainer.addView(balloonView)
        updateBalloonPosition(position)
        balloonContainer.visibility = View.VISIBLE
    }

    private fun updateFavoriteIcon(favBtn: ImageView, isFavorite: Boolean) {
        val iconRes = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        favBtn.setImageResource(iconRes)
    }

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
        balloonView.translationX = (pt.x - w / 2).toFloat()
        balloonView.translationY = (pt.y - h - offsetY).toFloat()
    }

    // ------------------------ 마커 처리 ------------------------

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

        val options = LabelOptions.from(LatLng.from(lat, lng)).setStyles(style)
        currentLabel?.remove()
        currentLabel = labelLayer.addLabel(options)
    }

    // 찜 별마커
    private fun addFavoriteLabel(lat: Double, lng: Double) {
        val bitmap = vectorToBitmap(R.drawable.ic_favorite)

        val sizePx = (20 * resources.displayMetrics.density).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)

        val labelLayer = kakaoMap?.labelManager?.layer ?: return
        val style = LabelStyle.from(scaled)
            .setAnchorPoint(0.5f, 0.6f)

        val options = LabelOptions.from(LatLng.from(lat, lng))
            .setStyles(style)
            .setTag("FAVORITE")

        val label = labelLayer.addLabel(options)
        favoriteLabels[lat to lng] = label
    }

    // ------------------------

    override fun onDestroyView() {
        super.onDestroyView()
        kakaoMap?.let {
            lastCameraPosition = it.cameraPosition
        }
        kakaoMap = null
        currentBalloonLatLng = null
        favoriteLabels.clear()
    }

    companion object {
        var lastCameraPosition: com.kakao.vectormap.camera.CameraPosition? = null
    }
}
