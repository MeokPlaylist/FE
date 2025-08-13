package com.example.meokpli.User.Category

/**
 * 피드 추천을 위한 카테고리 셋업(가정)
 * topic(큰주제) + categories(선택값들) 형태로 전송
 * 예: topic="분위기", categories=["전통적인","이색적인"]
 */
data class FeedCategorySetUpRequest(
    val topic: String,              // 큰 주제 (ex. "분위기")
    val categories: List<String>    // 선택한 카테고리들
)