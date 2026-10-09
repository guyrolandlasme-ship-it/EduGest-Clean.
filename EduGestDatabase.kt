package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentEntity::class,
        GradeEntity::class,
        AttendanceEntity::class,
        DisciplineEntity::class,
        ReceiptEntity::class,
        NotificationEntity::class,
        ChatMessageEntity::class,
        ConversationEntity::class,
        UserAccountEntity::class,
        TimetableSlotEntity::class,
        HomeworkExerciseEntity::class,
        OnlineCourseEntity::class,
        TextbookEntryEntity::class,
        GateCheckInEntity::class,
        SchoolTenantEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class EduGestDatabase : RoomDatabase() {
    abstract fun dao(): EduGestDao

    companion object {
        @Volatile
        private var INSTANCE: EduGestDatabase? = null

        fun getDatabase(context: Context): EduGestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduGestDatabase::class.java,
                    "edugest_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
