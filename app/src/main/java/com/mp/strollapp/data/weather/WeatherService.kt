package com.mp.strollapp.data.weather

import com.mp.strollapp.data.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// 기상청 단기예보 API 호출을 위한 Retrofit 인터페이스 정의
interface WeatherService {

    // 단기예보 조회 API (getVilageFcst)에 GET 요청을 보냄
    @GET("getVilageFcst")
    suspend fun getForecast(
        @Query("serviceKey", encoded = true) serviceKey: String,
        @Query("numOfRows") numOfRows: Int,
        @Query("pageNo") pageNo: Int,
        @Query("dataType") dataType: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): Response<WeatherResponse>
}