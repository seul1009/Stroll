package com.mp.strollapp.data.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// WeatherAPI 객체는 기상청 날씨 API에 접근하기 위한 Retrofit 인스턴스를 생성
object WeatherAPI {
    // 기상청 단기예보 API 기본 URL
    private const val BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/"

    val api: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }
}
