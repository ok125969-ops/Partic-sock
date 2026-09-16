package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UltronDao {
    @Query("SELECT * FROM ultron_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UltronMemoryEntity>>

    @Query("SELECT * FROM ultron_memory WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<UltronMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UltronMemoryEntity)

    @Query("DELETE FROM ultron_memory WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM ultron_memory")
    suspend fun clearAllMemories()
}
