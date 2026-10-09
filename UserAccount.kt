package com.example.data.model

data class UserAccount(
    val id: String,
    val username: String, // Code d'accès / Identifiant (ex: LNGR@1997)
    val password: String,
    val fullName: String,
    val role: UserRole,
    val email: String = "",
    val phone: String = "",
    val associatedStudentId: Int? = null,
    val subjectId: Int? = null,
    val className: String = "3e A",
    val studentClass: String = "3e A",
    val controlledClasses: String = "3e A",
    val taughtSubjects: String = "Général",
    val attachedStudents: String = "Élève 3e A",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
