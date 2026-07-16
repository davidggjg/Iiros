package com.iiros.scanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class ScanTypeConverters {
    @TypeConverter
    fun fromScanType(value: ScanType): String = value.name

    @TypeConverter
    fun toScanType(value: String): ScanType = ScanType.valueOf(value)
}

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(ScanTypeConverters::class)
abstract class ScanHistoryDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var instance: ScanHistoryDatabase? = null

        fun getInstance(context: Context): ScanHistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanHistoryDatabase::class.java,
                    "scan_history.db",
                ).build().also { instance = it }
            }
    }
}
