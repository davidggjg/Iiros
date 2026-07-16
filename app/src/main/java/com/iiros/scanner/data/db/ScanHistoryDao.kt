package com.iiros.scanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entity: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY scannedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}
