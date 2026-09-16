package com.example.data

import kotlinx.coroutines.flow.Flow

class UltronRepository(private val dao: UltronDao) {
    val allMemories: Flow<List<UltronMemoryEntity>> = dao.getAllMemories()

    fun getMemoriesByCategory(category: String): Flow<List<UltronMemoryEntity>> =
        dao.getMemoriesByCategory(category)

    suspend fun insertMemory(memory: UltronMemoryEntity) = dao.insertMemory(memory)

    suspend fun deleteMemory(id: Long) = dao.deleteMemoryById(id)

    suspend fun clearAll() = dao.clearAllMemories()
}
