package com.mp.strollapp.data.walk

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WalkRecordDao {
    @Insert
    suspend fun insert(record: WalkRecordEntity)

    @Query("SELECT * FROM walk_record ORDER BY timestamp DESC")
    suspend fun getAll(): List<WalkRecordEntity>

    // 오늘의 산책 데이터
    @Query("SELECT * FROM walk_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    suspend fun getTodayRecords(): List<WalkRecordEntity>

    @Query("DELETE FROM walk_record")
    suspend fun deleteAll()
}
