package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalHistoryDao {
    @Query("SELECT * FROM signal_history ORDER BY createdAtEpochMs DESC LIMIT 100")
    fun observeAll(): Flow<List<SignalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SignalHistoryEntity): Long

    @Query("UPDATE signal_history SET outcome = :outcome WHERE id = :id")
    suspend fun updateOutcome(id: Long, outcome: String)

    @Query("DELETE FROM signal_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM signal_history")
    suspend fun clearAll()
}
