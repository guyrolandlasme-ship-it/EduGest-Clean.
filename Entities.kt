package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val className: String,
    val parentName: String,
    val tuitionFee: Long,
    val paidAmount: Long,
    val whatsappNumber: String = "",
    val matricule: String = "",
    val schoolId: String = "gs-lasme-abidjan",
    val isAccessSuspended: Boolean = false,
    val directorWaiver: Boolean = false
)

@Entity(
    tableName = "grades",
    primaryKeys = ["studentId", "subjectId", "trimester"]
)
data class GradeEntity(
    val studentId: Int,
    val subjectId: Int,
    val trimester: Int,
    val score: Double
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val studentId: Int,
    val status: String // "P", "A", "R"
)

@Entity(tableName = "discipline")
data class DisciplineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Int,
    val studentName: String,
    val reason: String,
    val timestamp: Long
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val studentId: Int,
    val studentName: String,
    val amount: Long,
    val timestamp: Long,
    val cashierName: String,
    val paymentMethod: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetRole: String?,
    val targetStudentId: Int?,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String,
    val isRead: Boolean
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val recipientId: String,
    val recipientName: String,
    val content: String,
    val timestamp: Long,
    val status: String // "SENDING", "SENT", "DELIVERED", "READ"
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactRole: String,
    val contactId: String,
    val topic: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int
)

@Entity(tableName = "users")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val password: String,
    val fullName: String,
    val role: String,
    val email: String,
    val phone: String,
    val associatedStudentId: Int?,
    val subjectId: Int?,
    val className: String,
    val studentClass: String = "3e A",
    val controlledClasses: String = "3e A",
    val taughtSubjects: String = "Général",
    val attachedStudents: String = "Élève 3e A",
    val createdAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,
    val day: String,
    val startTime: String,
    val endTime: String,
    val subjectName: String,
    val teacherName: String,
    val classroom: String
)

@Entity(tableName = "homework_exercises")
data class HomeworkExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String,
    val className: String,
    val teacherName: String,
    val dueDate: String,
    val description: String,
    val pdfFileName: String,
    val pdfFileSize: String,
    val timestamp: Long
)

@Entity(tableName = "online_courses")
data class OnlineCourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,
    val subjectName: String,
    val teacherName: String,
    val topic: String,
    val meetingUrl: String,
    val startedAt: Long,
    val isActive: Boolean,
    val targetStudentsCount: Int,
    val whatsappSentCount: Int
)

@Entity(tableName = "textbook_entries")
data class TextbookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolId: String = "gs-lasme-abidjan",
    val className: String,
    val subjectName: String,
    val teacherName: String,
    val sessionDate: String,
    val chapterTitle: String,
    val objectivesAndSummary: String,
    val homeworkAssigned: String,
    val progressPercentage: Int,
    val isDirectorValidated: Boolean,
    val directorComment: String,
    val timestamp: Long
)

@Entity(tableName = "gate_checkins")
data class GateCheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolId: String = "gs-lasme-abidjan",
    val studentId: Int,
    val studentName: String,
    val className: String,
    val matricule: String,
    val parentWhatsApp: String,
    val checkInTime: Long,
    val gateType: String,
    val whatsappAlertSent: Boolean,
    val securityAgentName: String
)

@Entity(tableName = "school_tenants")
data class SchoolTenantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val city: String,
    val phone: String,
    val waveMerchantId: String,
    val orangeMerchantId: String,
    val studentsCount: Int,
    val teachersCount: Int
)

