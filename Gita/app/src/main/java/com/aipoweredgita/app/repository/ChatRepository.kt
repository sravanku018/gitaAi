package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.VoiceChatMessage
import com.aipoweredgita.app.database.VoiceChatMessageDao

class ChatRepository(private val dao: VoiceChatMessageDao) {
    suspend fun getAllMessages(): List<VoiceChatMessage> = dao.getAllMessages()
    suspend fun getRecentMessages(limit: Int): List<VoiceChatMessage> = dao.getRecentMessages(limit)
    suspend fun insertMessage(message: VoiceChatMessage) = dao.insertMessage(message)
    suspend fun deleteAllMessages() = dao.deleteAllMessages()
    suspend fun deleteMessageById(id: String) = dao.deleteMessageById(id)
}
