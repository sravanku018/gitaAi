package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.SpacedRepetitionDao
import com.aipoweredgita.app.database.SpacedRepetitionItem
import kotlinx.coroutines.flow.Flow

class SpacedRepetitionRepository(private val dao: SpacedRepetitionDao) {
    suspend fun insertItem(item: SpacedRepetitionItem): Long = dao.insertItem(item)
    suspend fun updateItem(item: SpacedRepetitionItem) = dao.updateItem(item)
    suspend fun deleteItem(id: Int) = dao.deleteItem(id)
    fun getDueItems(): Flow<List<SpacedRepetitionItem>> = dao.getDueItems()
    fun getAllActiveItems(): Flow<List<SpacedRepetitionItem>> = dao.getAllActiveItems()
}
