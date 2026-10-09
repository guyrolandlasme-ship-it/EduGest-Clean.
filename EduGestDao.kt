package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EduGestDao {
    // Students
    @Query("SELECT * FROM students ORDER BY id ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("UPDATE students SET paidAmount = paidAmount + :amount WHERE id = :studentId")
    suspend fun addPayment(studentId: Int, amount: Long)

    // Grades
    @Query("SELECT * FROM grades")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGrade(grade: GradeEntity)

    // Attendance
    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(list: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAttendance(attendance: AttendanceEntity)

    // Discipline
    @Query("SELECT * FROM discipline ORDER BY timestamp DESC")
    fun getAllDiscipline(): Flow<List<DisciplineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscipline(item: DisciplineEntity)

    // Receipts
    @Query("SELECT * FROM receipts ORDER BY id DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET status = :status WHERE conversationId = :convId AND senderRole != :viewerRole")
    suspend fun markMessagesAsRead(convId: String, status: String = "READ", viewerRole: String)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastTimestamp = :timestamp WHERE id = :convId")
    suspend fun updateConversationLastMessage(convId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :convId")
    suspend fun clearConversationUnread(convId: String)

    // User Accounts & Authentication
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserAccountEntity>)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("UPDATE users SET password = :newPass WHERE id = :userId")
    suspend fun updateUserPassword(userId: String, newPass: String)

    // Timetable Slots
    @Query("SELECT * FROM timetable_slots ORDER BY id ASC")
    fun getAllTimetableSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE className = :className ORDER BY id ASC")
    fun getTimetableSlotsForClass(className: String): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlots(slots: List<TimetableSlotEntity>)

    @Query("DELETE FROM timetable_slots WHERE id = :slotId")
    suspend fun deleteTimetableSlot(slotId: Long)

    // Homework Exercises
    @Query("SELECT * FROM homework_exercises ORDER BY timestamp DESC")
    fun getAllHomeworkExercises(): Flow<List<HomeworkExerciseEntity>>

    @Query("SELECT * FROM homework_exercises WHERE className = :className ORDER BY timestamp DESC")
    fun getHomeworkExercisesForClass(className: String): Flow<List<HomeworkExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkExercise(exercise: HomeworkExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkExercises(exercises: List<HomeworkExerciseEntity>)

    @Query("DELETE FROM homework_exercises WHERE id = :exerciseId")
    suspend fun deleteHomeworkExercise(exerciseId: Long)

    // Online Courses & WhatsApp Sessions
    @Query("SELECT * FROM online_courses ORDER BY startedAt DESC")
    fun getAllOnlineCourses(): Flow<List<OnlineCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineCourse(course: OnlineCourseEntity): Long

    @Query("UPDATE online_courses SET isActive = 0 WHERE id = :courseId")
    suspend fun closeOnlineCourse(courseId: Long)

    @Query("DELETE FROM online_courses WHERE id = :courseId")
    suspend fun deleteOnlineCourse(courseId: Long)

    @Query("UPDATE students SET whatsappNumber = :whatsapp WHERE id = :studentId")
    suspend fun updateStudentWhatsApp(studentId: Int, whatsapp: String)

    @Query("UPDATE students SET className = :className, whatsappNumber = :whatsapp WHERE id = :studentId")
    suspend fun updateStudentClassAndWhatsApp(studentId: Int, className: String, whatsapp: String)

    @Query("UPDATE students SET isAccessSuspended = :isSuspended WHERE id = :studentId")
    suspend fun updateStudentAccessSuspension(studentId: Int, isSuspended: Boolean)

    @Query("UPDATE students SET directorWaiver = :waiver WHERE id = :studentId")
    suspend fun updateStudentDirectorWaiver(studentId: Int, waiver: Boolean)

    @Query("UPDATE students SET matricule = :matricule WHERE id = :studentId")
    suspend fun updateStudentMatricule(studentId: Int, matricule: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    // Textbook / Cahier de Texte
    @Query("SELECT * FROM textbook_entries ORDER BY timestamp DESC")
    fun getAllTextbookEntries(): Flow<List<TextbookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextbookEntry(entry: TextbookEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextbookEntries(entries: List<TextbookEntryEntity>)

    @Query("UPDATE textbook_entries SET isDirectorValidated = 1, directorComment = :comment WHERE id = :id")
    suspend fun validateTextbookEntry(id: Long, comment: String)

    @Query("DELETE FROM textbook_entries WHERE id = :id")
    suspend fun deleteTextbookEntry(id: Long)

    // Gate CheckIn (Pointage & Badge QR au portail)
    @Query("SELECT * FROM gate_checkins ORDER BY checkInTime DESC")
    fun getAllGateCheckIns(): Flow<List<GateCheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGateCheckIn(record: GateCheckInEntity): Long

    // Multi-Tenant SchoolTenants
    @Query("SELECT * FROM school_tenants ORDER BY id ASC")
    fun getAllSchoolTenants(): Flow<List<SchoolTenantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolTenant(tenant: SchoolTenantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolTenants(tenants: List<SchoolTenantEntity>)
}
