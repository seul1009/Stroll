package com.mp.strollapp.data.weather

object GpsUtil {
    //위도(lat_X)와 경도(lng_Y)를 입력받아 기상청 API에서 사용하는 격자 좌표계(nx, ny)로 변환해 반환하는 함수
    fun convertGRID_GPS(lat_X: Double, lng_Y: Double): Map<String, Int> {
        val RE = 6371.00877
        val GRID = 5.0
        val SLAT1 = 30.0
        val SLAT2 = 60.0
        val OLON = 126.0
        val OLAT = 38.0
        val XO = 43
        val YO = 136

        val DEGRAD = Math.PI / 180.0
        val RADDEG = 180.0 / Math.PI
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        val sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        val sn2 = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        val sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        val sf2 = Math.pow(sf, sn2) * Math.cos(slat1) / sn2
        val ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        val ro2 = re * sf2 / Math.pow(ro, sn2)

        val rs = mutableMapOf<String, Int>()

        // 변환할 위도(lat_X), 경도(lng_Y)를 이용한 계산
        val ra = Math.tan(Math.PI * 0.25 + lat_X * DEGRAD * 0.5)
        val ra2 = re * sf2 / Math.pow(ra, sn2)
        val theta = lng_Y * DEGRAD - olon
        val theta2 = if (theta > Math.PI) theta - 2.0 * Math.PI else if (theta < -Math.PI) theta + 2.0 * Math.PI else theta
        val theta3 = theta2 * sn2

        // 격자 좌표 계산 (소수점 반올림)
        rs["nx"] = (ra2 * Math.sin(theta3) + XO + 0.5).toInt()
        rs["ny"] = (ro2 - ra2 * Math.cos(theta3) + YO + 0.5).toInt()
        return rs
    }
}
