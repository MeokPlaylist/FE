package com.example.meokpli.Main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.meokpli.R
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory

class StarFragment : Fragment() {

    private var kakaoMap: KakaoMap? = null
    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_star, container, false)
        mapView = root.findViewById(R.id.map_view)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // Fragment 종료 시 자원 해제
            }

            override fun onMapError(error: Exception?) {
                Log.e("StarFragment", "지도 로딩 실패: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                val center = LatLng.from(33.450701, 126.570667)
                map.moveCamera(CameraUpdateFactory.newCenterPosition(center))
                map.moveCamera(CameraUpdateFactory.zoomTo(3))
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        kakaoMap = null
    }
}
