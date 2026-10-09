package com.example.data.remote

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreChatService {

    companion object {
        const val FIRESTORE_DATABASE_ID = "ai-studio-android-edugest-3704e4bc-1406-49ca-a2dd-b093f1d9faf7"
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val app = FirebaseApp.getInstance()
            FirebaseFirestore.getInstance(app, FIRESTORE_DATABASE_ID)
        } catch (e: Exception) {
            Log.w("FirestoreChatService", "Firestore not available: ${e.message}")
            null
        }
    }

    val isAvailable: Boolean
        get() = firestore != null

    fun listenToConversationMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreChatService", "Snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChatMessage(
                                id = doc.getString("id") ?: doc.id,
                                conversationId = doc.getString("conversationId") ?: conversationId,
                                senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "",
                                senderRole = try {
                                    UserRole.valueOf(doc.getString("senderRole") ?: "DIRECTION")
                                } catch (e: Exception) {
                                    UserRole.DIRECTION
                                },
                                recipientId = doc.getString("recipientId") ?: "",
                                recipientName = doc.getString("recipientName") ?: "",
                                content = doc.getString("content") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                status = try {
                                    MessageStatus.valueOf(doc.getString("status") ?: "DELIVERED")
                                } catch (e: Exception) {
                                    MessageStatus.DELIVERED
                                }
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    fun sendMessage(message: ChatMessage) {
        val db = firestore ?: return
        val map = hashMapOf(
            "id" to message.id,
            "conversationId" to message.conversationId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "senderRole" to message.senderRole.name,
            "recipientId" to message.recipientId,
            "recipientName" to message.recipientName,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "status" to message.status.name
        )

        db.collection("conversations")
            .document(message.conversationId)
            .collection("messages")
            .document(message.id)
            .set(map)
            .addOnSuccessListener {
                Log.d("FirestoreChatService", "Message ${message.id} saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreChatService", "Failed to save message to Firestore", e)
            }

        val convSummary = hashMapOf(
            "lastMessage" to message.content,
            "lastTimestamp" to message.timestamp
        )
        db.collection("conversations")
            .document(message.conversationId)
            .set(convSummary, SetOptions.merge())
    }

    fun updateMessageStatus(conversationId: String, messageId: String, status: MessageStatus) {
        val db = firestore ?: return
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .update("status", status.name)
            .addOnFailureListener { e ->
                Log.w("FirestoreChatService", "Failed to update status on Firestore", e)
            }
    }
}
