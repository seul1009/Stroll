package com.mp.strollapp.data.walk

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room 데이터베이스 정의: WalkRecordEntity를 엔티티로 사용
@Database(entities = [WalkRecordEntity::class], version = 2)
abstract class WalkRecordDatabase : RoomDatabase() {
    abstract fun walkRecordDao(): WalkRecordDao

    companion object {
        @Volatile
        private var INSTANCE: WalkRecordDatabase? = null

        fun getInstance(context: Context): WalkRecordDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WalkRecordDatabase::class.java, // 데이터베이스 클래스 지정
                    "walk_record.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}
