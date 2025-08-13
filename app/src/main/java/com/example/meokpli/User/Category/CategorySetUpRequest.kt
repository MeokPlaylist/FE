package com.example.meokpli.User.category

/**
 * ⚠️ 필드명은 백엔드 DTO와 동일해야 함.
 * 아래는 일반적인 구조로 가정:
 * - 분위기/음식/동반자: 다중 선택
 * - 지역: 선택했다면 다중 전달(없으면 빈 배열)
 */
data class CategorySetUpRequest(
    val moods: List<String>,        // = "분위기" 선택값들
    val foods: List<String>,        // = "음식" 선택값들
    val companions: List<String>,   // = "동반자" 선택값들
    val regions: List<String> = emptyList() // = "지역" (선택 안했으면 빈배열)
)