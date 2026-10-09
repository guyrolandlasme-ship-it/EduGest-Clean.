package com.example

import com.example.data.model.*
import com.example.data.repository.EduGestRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testStudentTuitionRemainingAndStatus() {
        val studentPaid = Student(
            id = 0,
            name = "Awa Diallo",
            className = "3e A",
            parentName = "Famille Diallo",
            tuitionFee = 450_000L,
            paidAmount = 450_000L
        )
        assertEquals(0L, studentPaid.remainingBalance)
        assertEquals(PaymentStatus.SOLDE, studentPaid.paymentStatus)

        val studentPartial = Student(
            id = 1,
            name = "Karim Traoré",
            className = "3e A",
            parentName = "Famille Traoré",
            tuitionFee = 450_000L,
            paidAmount = 300_000L
        )
        assertEquals(150_000L, studentPartial.remainingBalance)
        assertEquals(PaymentStatus.PARTIEL, studentPartial.paymentStatus)

        val studentUnpaid = Student(
            id = 4,
            name = "Inès Bernard",
            className = "3e A",
            parentName = "Famille Bernard",
            tuitionFee = 450_000L,
            paidAmount = 0L
        )
        assertEquals(450_000L, studentUnpaid.remainingBalance)
        assertEquals(PaymentStatus.IMPAYE, studentUnpaid.paymentStatus)
    }

    @Test
    fun testAmountFormatting() {
        val formatted = EduGestRepository.formatAmount(450_000L)
        assertTrue(formatted.contains("450") && formatted.contains("000"))
    }

    @Test
    fun testMessageStatusProgression() {
        val msg = ChatMessage(
            id = "test_msg_1",
            conversationId = "conv_1",
            senderId = "direction",
            senderName = "Direction",
            senderRole = UserRole.DIRECTION,
            recipientId = "parent_0",
            recipientName = "Famille Diallo",
            content = "Félicitations pour le bulletin !",
            status = MessageStatus.SENT
        )
        assertEquals(MessageStatus.SENT, msg.status)
        val delivered = msg.copy(status = MessageStatus.DELIVERED)
        assertEquals(MessageStatus.DELIVERED, delivered.status)
        val read = delivered.copy(status = MessageStatus.READ)
        assertEquals(MessageStatus.READ, read.status)
    }

    @Test
    fun testUserAccountRolesAndTabs() {
        val adminUser = UserAccount(
            id = "usr_admin_lasme",
            username = "LNGR@1997",
            password = "LASME1997",
            fullName = "LASME",
            role = UserRole.DIRECTION
        )
        assertTrue(adminUser.role.tabs.contains("Comptes & Accès"))
        assertTrue(adminUser.role.tabs.contains("Administration"))

        val teacherUser = UserAccount(
            id = "usr_2",
            username = "konate.maths",
            password = "prof123",
            fullName = "M. Konaté",
            role = UserRole.ENSEIGNANT,
            subjectId = 0
        )
        assertTrue(teacherUser.role.tabs.contains("Notes"))

        val educUser = UserAccount(
            id = "usr_3",
            username = "educ.horizon",
            password = "educ123",
            fullName = "M. Cissé",
            role = UserRole.EDUCATEUR,
            controlledClasses = "6e A, 5e A, 4e A, 3e A"
        )
        assertFalse(educUser.role.tabs.contains("Discipline"))
        assertTrue(educUser.role.tabs.contains("Emploi du temps"))
        assertTrue(educUser.role.tabs.contains("Présence"))

        val studentUser = UserAccount(
            id = "usr_4",
            username = "eleve.awa",
            password = "eleve123",
            fullName = "Awa Diallo",
            role = UserRole.ELEVE,
            associatedStudentId = 0,
            studentClass = "3e A"
        )
        assertTrue(studentUser.role.tabs.contains("Mon Bulletin"))
        assertTrue(studentUser.role.tabs.contains("Devoirs & PDF"))
    }

    @Test
    fun testOnlineCourseSessionAndWhatsAppDiffusion() {
        val student6e1 = Student(
            id = 10,
            name = "Emmanuel Lasme",
            className = "6e 1",
            parentName = "M. Roland Lasme",
            whatsappNumber = "+225 07 89 45 12 30"
        )
        assertEquals("6e 1", student6e1.className)
        assertEquals("+225 07 89 45 12 30", student6e1.whatsappNumber)

        val course = OnlineCourseSession(
            id = 1L,
            className = "6e 1",
            subjectName = "Mathématiques",
            teacherName = "M. Konaté",
            topic = "Calcul littéral",
            meetingUrl = "https://meet.google.com/lasme-6e1-maths",
            targetStudentsCount = 5,
            whatsappSentCount = 5
        )
        assertTrue(course.isActive)
        assertEquals("6e 1", course.className)
        assertEquals(5, course.whatsappSentCount)

        // Verify tabs
        assertTrue(UserRole.ENSEIGNANT.tabs.contains("Cours en ligne"))
        assertTrue(UserRole.DIRECTION.tabs.contains("Cours en ligne"))
        assertTrue(UserRole.ELEVE.tabs.contains("Cours en ligne"))
        assertTrue(UserRole.PARENT.tabs.contains("Cours en ligne"))

        // Verify initial student dataset has 6e 1 with WhatsApp
        val studentsIn6e1 = EduGestRepository.initialStudents.filter { it.className == "6e 1" }
        assertTrue(studentsIn6e1.isNotEmpty())
        assertTrue(studentsIn6e1.all { it.whatsappNumber.startsWith("+225") })
    }
}
