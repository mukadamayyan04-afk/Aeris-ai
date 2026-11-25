package com.example.data.repository

import com.example.data.database.MemoryDao
import com.example.data.database.entities.MemoryEntity
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MemoryRepository {
    fun getAllMemories(): Flow<List<MemoryItem>>
    fun searchMemories(query: String): Flow<List<MemoryItem>>
    suspend fun saveMemory(memory: MemoryItem): Long
    suspend fun updateMemory(memory: MemoryItem)
    suspend fun deleteMemory(id: Long)
    suspend fun clearAllMemories()
}

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getAllMemories(): Flow<List<MemoryItem>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchMemories(query: String): Flow<List<MemoryItem>> {
        return memoryDao.searchMemories(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMemory(memory: MemoryItem): Long {
        return memoryDao.insert(MemoryEntity.fromDomain(memory))
    }

    override suspend fun updateMemory(memory: MemoryItem) {
        memoryDao.update(MemoryEntity.fromDomain(memory))
    }

    override suspend fun deleteMemory(id: Long) {
        memoryDao.deleteById(id)
    }

    override suspend fun clearAllMemories() {
        memoryDao.clearAll()
    }
}
