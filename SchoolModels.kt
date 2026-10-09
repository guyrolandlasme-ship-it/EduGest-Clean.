package com.example.data.model

data class Student(
    val id: Int,
    val name: String,
    val className: String = "3e A",
    val parentName: String,
    val tuitionFee: Long = 450_000L,
    val paidAmount: Long = 0L,
    val whatsappNumber: String = "",
    val matricule: String = "",
    val schoolId: String = "gs-lasme-abidjan",
    val isAccessSuspended: Boolean = false,
    val directorWaiver: Boolean = false
) {
    val remainingBalance: Long
        get() = (tuitionFee - paidAmount).coerceAtLeast(0L)

    val paymentStatus: PaymentStatus
        get() = when {
            paidAmount >= tuitionFee -> PaymentStatus.SOLDE
            paidAmount > 0L -> PaymentStatus.PARTIEL
            else -> PaymentStatus.IMPAYE
        }

    val canAccessOnlineContent: Boolean
        get() = !isAccessSuspended || directorWaiver || paymentStatus == PaymentStatus.SOLDE
}

enum class PaymentStatus(val label: String) {
    SOLDE("Soldé"),
    PARTIEL("Partiel"),
    IMPAYE("Impayé")
}

data class Subject(
    val id: Int,
    val name: String,
    val teacherName: String,
    val coef: Int
)

data class GradeEntry(
    val studentId: Int,
    val subjectId: Int,
    val trimester: Int, // 1, 2, 3
    val score: Double // 0 to 20
)

enum class AttendanceStatus(val code: String, val label: String) {
    PRESENT("P", "Présent"),
    ABSENT("A", "Absent"),
    RETARD("R", "Retard")
}

data class StudentAttendance(
    val studentId: Int,
    val studentName: String,
    val status: AttendanceStatus
)

data class DisciplineItem(
    val id: Long = 0,
    val studentId: Int,
    val studentName: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PaymentReceiptItem(
    val id: Long = 0,
    val receiptNumber: String,
    val studentId: Int,
    val studentName: String,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String = "Caisse Principale",
    val paymentMethod: String = "Espèces"
)

data class NotificationEntry(
    val id: Long = 0,
    val targetRole: String? = null, // null for all
    val targetStudentId: Int? = null,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "info", // "bulletin", "absence", "finance", "chat", "message"
    val isRead: Boolean = false
)

data class StudentReportCard(
    val student: Student,
    val trimester: Int,
    val subjectGrades: List<SubjectGradeDetail>,
    val overallAverage: Double,
    val rank: Int,
    val totalStudents: Int,
    val mention: String,
    val yearlyAverages: List<Double>,
    val annualAverage: Double
)

data class SubjectGradeDetail(
    val subject: Subject,
    val score: Double,
    val classAverage: Double
)

data class TimetableSlot(
    val id: Long = 0,
    val className: String = "3e A",
    val day: String = "Lundi",
    val startTime: String = "08:00",
    val endTime: String = "09:00",
    val subjectName: String = "Mathématiques",
    val teacherName: String = "M. Konaté",
    val classroom: String = "Salle 12"
)

data class HomeworkExercise(
    val id: Long = 0,
    val title: String,
    val subjectName: String,
    val className: String = "3e A",
    val teacherName: String,
    val dueDate: String,
    val description: String,
    val pdfFileName: String,
    val pdfFileSize: String = "1.2 Mo",
    val timestamp: Long = System.currentTimeMillis()
)

data class OnlineCourseSession(
    val id: Long = 0,
    val className: String, // ex: "6e 1"
    val subjectName: String, // ex: "Mathématiques"
    val teacherName: String, // ex: "M. Konaté"
    val topic: String, // ex: "Théorème de Thalès & Équations"
    val meetingUrl: String, // ex: "https://meet.google.com/lasme-6e1-maths"
    val startedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val targetStudentsCount: Int = 0,
    val whatsappSentCount: Int = 0
)

data class TextbookEntry(
    val id: Long = 0,
    val schoolId: String = "gs-lasme-abidjan",
    val className: String = "6e 1",
    val subjectName: String = "Mathématiques",
    val teacherName: String = "M. Konaté",
    val sessionDate: String = "03/10/2026",
    val chapterTitle: String,
    val objectivesAndSummary: String,
    val homeworkAssigned: String = "",
    val progressPercentage: Int = 65,
    val isDirectorValidated: Boolean = false,
    val directorComment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class GateCheckInRecord(
    val id: Long = 0,
    val schoolId: String = "gs-lasme-abidjan",
    val studentId: Int,
    val studentName: String,
    val className: String,
    val matricule: String,
    val parentWhatsApp: String,
    val checkInTime: Long = System.currentTimeMillis(),
    val gateType: String = "ENTREE", // "ENTREE", "SORTIE"
    val whatsappAlertSent: Boolean = true,
    val securityAgentName: String = "Portail Principal"
)

data class SchoolTenant(
    val id: String, // e.g. "gs-lasme-abidjan"
    val name: String, // "GROUPE SCOLAIRE LASME"
    val code: String, // "GSL-01"
    val city: String, // "Abidjan, Yopougon"
    val phone: String = "+225 01 02 03 04",
    val waveMerchantId: String = "WAVE-CI-77894",
    val orangeMerchantId: String = "OM-CI-22501",
    val studentsCount: Int = 13,
    val teachersCount: Int = 6
)

