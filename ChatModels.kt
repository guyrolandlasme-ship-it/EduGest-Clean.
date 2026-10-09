package com.example.data.model

enum class MessageStatus {
    SENDING,
    SENT,       // Single check
    DELIVERED,  // Double check (gray)
    READ        // Double check (teal/blue)
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val recipientId: String,
    val recipientName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)

data class Conversation(
    val id: String,
    val contactName: String,
    val contactRole: UserRole,
    val contactId: String,
    val topic: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0
)

data class ChatContact(
    val id: String,
    val name: String,
    val role: UserRole,
    val roleTitle: String,
    val detail: String // e.g. "Professeur de Mathématiques" or "Parent d'Awa Diallo"
)
