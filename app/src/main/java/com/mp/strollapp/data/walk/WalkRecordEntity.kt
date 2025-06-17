package com.mp.strollapp.data.walk

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room 데이터베이스에 저장될 테이블 walk_record의 엔티티 클래스 정의
@Entity(tableName = "walk_record")
data class WalkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // 산책 거리
    val distance: Double,
    // 산책 시간
    val duration: Int,
    // 산책 경로를 저장 (위도/경도)
    val path: String,
    // 산책 기록이 저장된 시각
    val timestamp: Long
)