package com.example.weathersnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.weathersnap.data.local.dao.ReportDao
import com.example.weathersnap.data.local.entity.ReportEntity

@Database(
    entities = [ReportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract val reportDao: ReportDao
}
