package com.mp.strollapp.data.weather.model

// 기상청 날씨 API 응답의 개별 예보 항목을 나타내는 데이터 클래스
data class Item(
    val category: String,   // 예보 항목 구분 코드
    val fcstDate: String,   // 예보 날짜
    val fcstTime: String,   // 예보 시간
    val fcstValue: String,  // 예보 값
    val nx: Int,            // 격자 X 좌표
    val ny: Int             // 격자 Y 좌표
)