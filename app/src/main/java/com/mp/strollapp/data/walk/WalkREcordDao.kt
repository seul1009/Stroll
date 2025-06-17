package com.mp.strollapp.data.walk

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Room DAO 인터페이스: walk_record 테이블에 접근하는 메서드 정의
@Dao
interface WalkRecordDao {
    @Insert
    suspend fun insert(record: WalkRecordEntity)

    // 모든 산책 기록을 timestamp 기준 내림차순으로 조회
    @Query("SELECT * FROM walk_record ORDER BY timestamp DESC")
    suspend fun getAll(): List<WalkRecordEntity>

    // 오늘의 산책 데이터
    @Query("SELECT * FROM walk_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    suspend fun getTodayRecords(): List<WalkRecordEntity>

    // 모든 산책 기록 삭제
    @Query("DELETE FROM walk_record")
    suspend fun deleteAll()
}
