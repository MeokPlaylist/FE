package com.meokpli.app.main.Roadmap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RoadmapEditViewModel : ViewModel() {
    // 여행 제목
    var tripTitle: String = ""

    // 편집 중 아이템 리스트 (프론트 상태 유지용)
    val editItems = MutableLiveData<List<RoadmapEditFragment.EditListItem>>(emptyList())
}
