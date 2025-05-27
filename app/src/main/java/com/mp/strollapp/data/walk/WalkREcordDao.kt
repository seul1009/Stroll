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
}
