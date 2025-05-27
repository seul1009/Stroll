package com.mp.strollapp.data.walk

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                    WalkRecordDatabase::class.java,
                    "walk_record.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}
