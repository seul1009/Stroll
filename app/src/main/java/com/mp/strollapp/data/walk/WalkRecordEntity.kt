package com.mp.strollapp.data.walk

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walk_record")
data class WalkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val distance: Double,
    val duration: Int,
    val path: String,
    val timestamp: Long
)