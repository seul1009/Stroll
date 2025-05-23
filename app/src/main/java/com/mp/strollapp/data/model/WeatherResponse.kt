package com.mp.strollapp.data.model

data class WeatherResponse(
    val response: ResponseBody
){

}

data class ResponseBody(
    val body: Body
)

data class Body(
    val items: Items
)

data class Items(
    val item: List<WeatherItem>
)

data class WeatherItem(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String,
    val nx: Int,
    val ny: Int
)
