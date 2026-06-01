package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.VoiceChatMessage
import com.aipoweredgita.app.database.VoiceChatMessageDao

class ChatRepository(private val dao: VoiceChatMessageDao) {
    suspend fun getAllMessages(): List<VoiceChatMessage> = dao.getAllMessages()
    suspend fun insertMessage(message: VoiceChatMessage) = dao.insertMessage(message)
    suspend fun deleteAllMessages() = dao.deleteAllMessages()
}
