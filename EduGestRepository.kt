package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.FirestoreChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EduGestRepository(
    private val dao: EduGestDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val firestoreService = FirestoreChatService()

    // Subject definitions
    val subjects: List<Subject> = listOf(
        Subject(id = 0, name = "Maths", teacherName = "M. Konaté", coef = 4),
        Subject(id = 1, name = "Français", teacherName = "Mme Dupont", coef = 4),
        Subject(id = 2, name = "Histoire-Géo", teacherName = "M. Sow", coef = 3),
        Subject(id = 3, name = "SVT", teacherName = "Mme Leroy", coef = 2),
        Subject(id = 4, name = "Anglais", teacherName = "M. Adams", coef = 2),
        Subject(id = 5, name = "Phys-Chimie", teacherName = "Mme Camara", coef = 3)
    )

    // Reactive streams
    val students: Flow<List<Student>> = dao.getAllStudents().map { list ->
        list.map {
            Student(
                id = it.id,
                name = it.name,
                className = it.className,
                parentName = it.parentName,
                tuitionFee = it.tuitionFee,
                paidAmount = it.paidAmount,
                whatsappNumber = it.whatsappNumber,
                matricule = it.matricule,
                schoolId = it.schoolId,
                isAccessSuspended = it.isAccessSuspended,
                directorWaiver = it.directorWaiver
            )
        }
    }

    val grades: Flow<List<GradeEntry>> = dao.getAllGrades().map { list ->
        list.map { GradeEntry(it.studentId, it.subjectId, it.trimester, it.score) }
    }

    val attendance: Flow<List<StudentAttendance>> = dao.getAllAttendance().map { list ->
        list.map { att ->
            val studentName = initialStudentNames.getOrElse(att.studentId) { "Élève ${att.studentId}" }
            val status = when (att.status) {
                "A" -> AttendanceStatus.ABSENT
                "R" -> AttendanceStatus.RETARD
                else -> AttendanceStatus.PRESENT
            }
            StudentAttendance(att.studentId, studentName, status)
        }
    }

    val discipline: Flow<List<DisciplineItem>> = dao.getAllDiscipline().map { list ->
        list.map { DisciplineItem(it.id, it.studentId, it.studentName, it.reason, it.timestamp) }
    }

    val receipts: Flow<List<PaymentReceiptItem>> = dao.getAllReceipts().map { list ->
        list.map {
            PaymentReceiptItem(
                id = it.id,
                receiptNumber = it.receiptNumber,
                studentId = it.studentId,
                studentName = it.studentName,
                amount = it.amount,
                timestamp = it.timestamp,
                cashierName = it.cashierName,
                paymentMethod = it.paymentMethod
            )
        }
    }

    val notifications: Flow<List<NotificationEntry>> = dao.getAllNotifications().map { list ->
        list.map {
            NotificationEntry(
                id = it.id,
                targetRole = it.targetRole,
                targetStudentId = it.targetStudentId,
                title = it.title,
                message = it.message,
                timestamp = it.timestamp,
                type = it.type,
                isRead = it.isRead
            )
        }
    }

    val conversations: Flow<List<Conversation>> = dao.getAllConversations().map { list ->
        list.map {
            Conversation(
                id = it.id,
                contactName = it.contactName,
                contactRole = try { UserRole.valueOf(it.contactRole) } catch (e: Exception) { UserRole.ENSEIGNANT },
                contactId = it.contactId,
                topic = it.topic,
                lastMessage = it.lastMessage,
                lastTimestamp = it.lastTimestamp,
                unreadCount = it.unreadCount
            )
        }
    }

    val users: Flow<List<UserAccount>> = dao.getAllUsers().map { list ->
        list.map {
            UserAccount(
                id = it.id,
                username = it.username,
                password = it.password,
                fullName = it.fullName,
                role = try { UserRole.valueOf(it.role) } catch (e: Exception) { UserRole.PARENT },
                email = it.email,
                phone = it.phone,
                associatedStudentId = it.associatedStudentId,
                subjectId = it.subjectId,
                className = it.className,
                studentClass = it.studentClass,
                controlledClasses = it.controlledClasses,
                taughtSubjects = it.taughtSubjects,
                attachedStudents = it.attachedStudents,
                createdAt = it.createdAt,
                isActive = it.isActive
            )
        }
    }

    val timetableSlots: Flow<List<TimetableSlot>> = dao.getAllTimetableSlots().map { list ->
        list.map {
            TimetableSlot(
                id = it.id,
                className = it.className,
                day = it.day,
                startTime = it.startTime,
                endTime = it.endTime,
                subjectName = it.subjectName,
                teacherName = it.teacherName,
                classroom = it.classroom
            )
        }
    }

    val homeworkExercises: Flow<List<HomeworkExercise>> = dao.getAllHomeworkExercises().map { list ->
        list.map {
            HomeworkExercise(
                id = it.id,
                title = it.title,
                subjectName = it.subjectName,
                className = it.className,
                teacherName = it.teacherName,
                dueDate = it.dueDate,
                description = it.description,
                pdfFileName = it.pdfFileName,
                pdfFileSize = it.pdfFileSize,
                timestamp = it.timestamp
            )
        }
    }

    val onlineCourses: Flow<List<OnlineCourseSession>> = dao.getAllOnlineCourses().map { list ->
        list.map {
            OnlineCourseSession(
                id = it.id,
                className = it.className,
                subjectName = it.subjectName,
                teacherName = it.teacherName,
                topic = it.topic,
                meetingUrl = it.meetingUrl,
                startedAt = it.startedAt,
                isActive = it.isActive,
                targetStudentsCount = it.targetStudentsCount,
                whatsappSentCount = it.whatsappSentCount
            )
        }
    }

    val textbookEntries: Flow<List<TextbookEntry>> = dao.getAllTextbookEntries().map { list ->
        list.map {
            TextbookEntry(
                id = it.id,
                schoolId = it.schoolId,
                className = it.className,
                subjectName = it.subjectName,
                teacherName = it.teacherName,
                sessionDate = it.sessionDate,
                chapterTitle = it.chapterTitle,
                objectivesAndSummary = it.objectivesAndSummary,
                homeworkAssigned = it.homeworkAssigned,
                progressPercentage = it.progressPercentage,
                isDirectorValidated = it.isDirectorValidated,
                directorComment = it.directorComment,
                timestamp = it.timestamp
            )
        }
    }

    val gateCheckIns: Flow<List<GateCheckInRecord>> = dao.getAllGateCheckIns().map { list ->
        list.map {
            GateCheckInRecord(
                id = it.id,
                schoolId = it.schoolId,
                studentId = it.studentId,
                studentName = it.studentName,
                className = it.className,
                matricule = it.matricule,
                parentWhatsApp = it.parentWhatsApp,
                checkInTime = it.checkInTime,
                gateType = it.gateType,
                whatsappAlertSent = it.whatsappAlertSent,
                securityAgentName = it.securityAgentName
            )
        }
    }

    val schoolTenants: Flow<List<SchoolTenant>> = dao.getAllSchoolTenants().map { list ->
        list.map {
            SchoolTenant(
                id = it.id,
                name = it.name,
                code = it.code,
                city = it.city,
                phone = it.phone,
                waveMerchantId = it.waveMerchantId,
                orangeMerchantId = it.orangeMerchantId,
                studentsCount = it.studentsCount,
                teachersCount = it.teachersCount
            )
        }
    }

    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>> {
        return dao.getMessagesForConversation(convId).map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    conversationId = it.conversationId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderRole = try { UserRole.valueOf(it.senderRole) } catch (e: Exception) { UserRole.DIRECTION },
                    recipientId = it.recipientId,
                    recipientName = it.recipientName,
                    content = it.content,
                    timestamp = it.timestamp,
                    status = try { MessageStatus.valueOf(it.status) } catch (e: Exception) { MessageStatus.DELIVERED }
                )
            }
        }
    }

    init {
        scope.launch {
            checkAndSeedDatabase()
        }
    }

    private suspend fun checkAndSeedDatabase() {
        val existingStudents = dao.getAllStudents().first()
        if (existingStudents.isEmpty()) {
            seedInitialData()
        }
        val existingUsers = dao.getAllUsers().first()
        // Purge de tous les anciens comptes démo
        val demoUsernames = setOf(
            "admin", "konate.maths", "dupont.fr", "educ.horizon",
            "caisse.horizon", "parent.awa", "parent.karim", "eleve.awa", "eleve.karim"
        )
        for (u in existingUsers) {
            if (u.username.lowercase().trim() in demoUsernames) {
                dao.deleteUser(u.id)
            }
        }
        // Toujours s'assurer que le compte administrateur LNGR@1997 / LASME1997 existe et est actif
        val adminUser = dao.getUserByUsername("LNGR@1997")
        if (adminUser == null) {
            seedInitialUsers()
        } else if (adminUser.password != "LASME1997" || !adminUser.isActive) {
            dao.insertUser(
                adminUser.copy(
                    password = "LASME1997",
                    fullName = "LASME",
                    role = "DIRECTION",
                    isActive = true
                )
            )
        }
        val existingSlots = dao.getAllTimetableSlots().first()
        if (existingSlots.isEmpty()) {
            seedInitialTimetable()
        }
        val existingHomework = dao.getAllHomeworkExercises().first()
        if (existingHomework.isEmpty()) {
            seedInitialHomework()
        }
        val existingCourses = dao.getAllOnlineCourses().first()
        if (existingCourses.isEmpty()) {
            seedInitialOnlineCourses()
        }
        val existingTenants = dao.getAllSchoolTenants().first()
        if (existingTenants.isEmpty()) {
            seedInitialSchoolTenants()
        }
        val existingTextbook = dao.getAllTextbookEntries().first()
        if (existingTextbook.isEmpty()) {
            seedInitialTextbook()
        }
        val existingCheckIns = dao.getAllGateCheckIns().first()
        if (existingCheckIns.isEmpty()) {
            seedInitialGateCheckIns()
        }
    }

    private suspend fun seedInitialUsers() {
        val now = System.currentTimeMillis()
        val adminAccount = UserAccountEntity(
            id = "usr_admin_lasme",
            username = "LNGR@1997",
            password = "LASME1997",
            fullName = "LASME",
            role = "DIRECTION",
            email = "direction@gs-lasme.ci",
            phone = "+225 01 02 03 04",
            associatedStudentId = null,
            subjectId = null,
            className = "3e A",
            studentClass = "",
            controlledClasses = "Toutes les classes (6e à Tle)",
            taughtSubjects = "Direction générale",
            attachedStudents = "",
            createdAt = now,
            isActive = true
        )
        dao.insertUser(adminAccount)
    }

    private suspend fun seedInitialTimetable() {
        val slots = mutableListOf<TimetableSlotEntity>()
        val defaultClasses = listOf("3e A", "6e A", "5e A", "4e A", "2nde C", "1ère D", "Tle D")

        val schedule3eA = listOf(
            Triple("Lundi", "08:00 - 10:00", Triple("Mathématiques", "M. Konaté", "Salle 12")),
            Triple("Lundi", "10:15 - 12:15", Triple("Français", "Mme Dupont", "Salle 12")),
            Triple("Lundi", "14:00 - 16:00", Triple("Anglais", "M. Kouamé", "Salle 12")),
            Triple("Mardi", "08:00 - 10:00", Triple("Histoire-Géo", "M. Bamba", "Salle 12")),
            Triple("Mardi", "10:15 - 12:15", Triple("Physique-Chimie", "M. Soro", "Labo Sciences")),
            Triple("Mardi", "14:00 - 16:00", Triple("Informatique", "M. Yao", "Salle Multimédia")),
            Triple("Mercredi", "08:00 - 10:00", Triple("SVT", "Mme Touré", "Labo SVT")),
            Triple("Mercredi", "10:15 - 12:15", Triple("EPS", "M. Bakayoko", "Terrain de sport")),
            Triple("Jeudi", "08:00 - 10:00", Triple("Français", "Mme Dupont", "Salle 12")),
            Triple("Jeudi", "10:15 - 12:15", Triple("Mathématiques", "M. Konaté", "Salle 12")),
            Triple("Jeudi", "14:00 - 16:00", Triple("Espagnol / Allemand", "Mme Traoré", "Salle 12")),
            Triple("Vendredi", "08:00 - 10:00", Triple("Anglais", "M. Kouamé", "Salle 12")),
            Triple("Vendredi", "10:15 - 12:15", Triple("Arts Plastiques / Musique", "M. N'Guessan", "Salle Arts"))
        )

        schedule3eA.forEach { (day, timeRange, subjInfo) ->
            val times = timeRange.split(" - ")
            slots.add(
                TimetableSlotEntity(
                    className = "3e A",
                    day = day,
                    startTime = times[0].trim(),
                    endTime = times[1].trim(),
                    subjectName = subjInfo.first,
                    teacherName = subjInfo.second,
                    classroom = subjInfo.third
                )
            )
        }

        // Also add baseline sample timetable for other classes
        defaultClasses.filter { it != "3e A" }.forEach { cls ->
            val sampleSlots = listOf(
                TimetableSlotEntity(className = cls, day = "Lundi", startTime = "08:00", endTime = "10:00", subjectName = "Français", teacherName = "Mme Dupont", classroom = "Salle $cls"),
                TimetableSlotEntity(className = cls, day = "Lundi", startTime = "10:15", endTime = "12:15", subjectName = "Mathématiques", teacherName = "M. Konaté", classroom = "Salle $cls"),
                TimetableSlotEntity(className = cls, day = "Mardi", startTime = "08:00", endTime = "10:00", subjectName = "Anglais", teacherName = "M. Kouamé", classroom = "Salle $cls"),
                TimetableSlotEntity(className = cls, day = "Mardi", startTime = "10:15", endTime = "12:15", subjectName = "SVT", teacherName = "Mme Touré", classroom = "Labo SVT"),
                TimetableSlotEntity(className = cls, day = "Jeudi", startTime = "08:00", endTime = "10:00", subjectName = "Histoire-Géo", teacherName = "M. Bamba", classroom = "Salle $cls"),
                TimetableSlotEntity(className = cls, day = "Vendredi", startTime = "08:00", endTime = "10:00", subjectName = "Physique-Chimie", teacherName = "M. Soro", classroom = "Labo Sciences")
            )
            slots.addAll(sampleSlots)
        }

        dao.insertTimetableSlots(slots)
    }

    private suspend fun seedInitialHomework() {
        val exercises = listOf(
            HomeworkExerciseEntity(
                title = "DM 2 : Fonctions affines & Théorème de Thalès",
                subjectName = "Mathématiques",
                className = "3e A",
                teacherName = "M. Konaté",
                dueDate = "10 Octobre 2026",
                description = "Exercices d'application directe sur les proportions, calculs de pentes et représentations graphiques. Rendre sur feuille double numérotée.",
                pdfFileName = "DM2_Maths_3eA_Thales_Fonctions.pdf",
                pdfFileSize = "1.4 Mo",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            ),
            HomeworkExerciseEntity(
                title = "Commentaire composé : Les Soleils des Indépendances",
                subjectName = "Français",
                className = "3e A",
                teacherName = "Mme Dupont",
                dueDate = "14 Octobre 2026",
                description = "Étude analytique de l'extrait du chapitre 3 d'Ahmadou Kourouma. Respecter le plan en deux parties équilibrées.",
                pdfFileName = "Devoir_Francais_3eA_Ahmadou_Kourouma.pdf",
                pdfFileSize = "850 Ko",
                timestamp = System.currentTimeMillis() - 86400000L
            ),
            HomeworkExerciseEntity(
                title = "TP N°1 : Oxydoréduction & Mesures de pH",
                subjectName = "Physique-Chimie",
                className = "3e A",
                teacherName = "M. Soro",
                dueDate = "18 Octobre 2026",
                description = "Compte-rendu de manipulations de laboratoire avec schémas légendés et équations bilans des réactions chimiques.",
                pdfFileName = "Fiche_TP_Chimie_Oxydoreduction_pH.pdf",
                pdfFileSize = "2.1 Mo",
                timestamp = System.currentTimeMillis()
            ),
            HomeworkExerciseEntity(
                title = "Fiche de synthèse : La Guerre Froide (1947-1991)",
                subjectName = "Histoire-Géographie",
                className = "3e A",
                teacherName = "M. Bamba",
                dueDate = "20 Octobre 2026",
                description = "Synthèse chronologique et carte des blocs bipolaires à compléter selon les consignes du document joint.",
                pdfFileName = "Synthese_HG_Guerre_Froide_3eA.pdf",
                pdfFileSize = "3.2 Mo",
                timestamp = System.currentTimeMillis()
            )
        )
        dao.insertHomeworkExercises(exercises)
    }

    private suspend fun seedInitialOnlineCourses() {
        val now = System.currentTimeMillis()
        val initialCourse = OnlineCourseEntity(
            id = 1L,
            className = "6e 1",
            subjectName = "Mathématiques",
            teacherName = "M. Konaté",
            topic = "Introduction aux fractions et nombres décimaux",
            meetingUrl = "https://meet.google.com/lasme-6e1-maths",
            startedAt = now - 86400000L,
            isActive = false,
            targetStudentsCount = 5,
            whatsappSentCount = 5
        )
        dao.insertOnlineCourse(initialCourse)
    }

    private suspend fun seedInitialSchoolTenants() {
        val tenants = listOf(
            SchoolTenantEntity(
                id = "gs-lasme-abidjan",
                name = "GROUPE SCOLAIRE LASME",
                code = "GSL-01",
                city = "Abidjan, Yopougon",
                phone = "+225 01 02 03 04",
                waveMerchantId = "WAVE-CI-77894",
                orangeMerchantId = "OM-CI-22501",
                studentsCount = 13,
                teachersCount = 6
            ),
            SchoolTenantEntity(
                id = "college-horizon-plateau",
                name = "COLLÈGE HORIZON EXCELLENCE",
                code = "CHE-02",
                city = "Abidjan, Plateau",
                phone = "+225 05 10 20 30",
                waveMerchantId = "WAVE-CI-88901",
                orangeMerchantId = "OM-CI-33412",
                studentsCount = 450,
                teachersCount = 28
            ),
            SchoolTenantEntity(
                id = "lycee-st-jean-cocody",
                name = "LYCÉE PRIVÉ SAINT-JEAN",
                code = "LPSJ-03",
                city = "Abidjan, Cocody",
                phone = "+225 07 40 50 60",
                waveMerchantId = "WAVE-CI-99432",
                orangeMerchantId = "OM-CI-44589",
                studentsCount = 620,
                teachersCount = 42
            )
        )
        dao.insertSchoolTenants(tenants)
    }

    private suspend fun seedInitialTextbook() {
        val now = System.currentTimeMillis()
        val entries = listOf(
            TextbookEntryEntity(
                id = 1L,
                schoolId = "gs-lasme-abidjan",
                className = "6e 1",
                subjectName = "Mathématiques",
                teacherName = "M. Konaté",
                sessionDate = "02/10/2026",
                chapterTitle = "Chapitre 1 : Nombres décimaux et repérage",
                objectivesAndSummary = "Définition de l'écriture décimale, troncature et comparaison de fractions simples. Exercices d'application réalisés en classe.",
                homeworkAssigned = "Exercices 12 et 14 page 32 du manuel officiel.",
                progressPercentage = 75,
                isDirectorValidated = true,
                directorComment = "Progression conforme aux programmes officiels. Bon rythme.",
                timestamp = now - 86400000L
            ),
            TextbookEntryEntity(
                id = 2L,
                schoolId = "gs-lasme-abidjan",
                className = "6e 1",
                subjectName = "Français",
                teacherName = "Mme Dupont",
                sessionDate = "01/10/2026",
                chapterTitle = "Module 1 : Le récit d'aventure et figures de style",
                objectivesAndSummary = "Lecture méthodique d'un extrait de Robinson Crusoé. Analyse du lexique de la survie et temps du récit.",
                homeworkAssigned = "Rédiger 10 lignes décrivant l'arrivée sur l'île.",
                progressPercentage = 60,
                isDirectorValidated = false,
                directorComment = "",
                timestamp = now - 86400000L * 2
            ),
            TextbookEntryEntity(
                id = 3L,
                schoolId = "gs-lasme-abidjan",
                className = "3e A",
                subjectName = "Mathématiques",
                teacherName = "M. Konaté",
                sessionDate = "02/10/2026",
                chapterTitle = "Théorème de Thalès et réciproque",
                objectivesAndSummary = "Démonstration des rapports de projection et configurations triangulaires emboîtées.",
                homeworkAssigned = "DM 2 à rendre sur feuille double.",
                progressPercentage = 80,
                isDirectorValidated = true,
                directorComment = "Validé par la Direction.",
                timestamp = now - 3600000L * 5
            )
        )
        dao.insertTextbookEntries(entries)
    }

    private suspend fun seedInitialGateCheckIns() {
        val now = System.currentTimeMillis()
        val records = listOf(
            GateCheckInEntity(
                id = 1L,
                schoolId = "gs-lasme-abidjan",
                studentId = 0,
                studentName = "Emmanuel Lasme",
                className = "6e 1",
                matricule = "MAT-2026-6E1-001",
                parentWhatsApp = "+225 07 89 45 12 30",
                checkInTime = now - 7200000L,
                gateType = "ENTREE",
                whatsappAlertSent = true,
                securityAgentName = "Portail Principal"
            ),
            GateCheckInEntity(
                id = 2L,
                schoolId = "gs-lasme-abidjan",
                studentId = 1,
                studentName = "Aminata Coulibaly",
                className = "6e 1",
                matricule = "MAT-2026-6E1-002",
                parentWhatsApp = "+225 05 66 77 88 99",
                checkInTime = now - 6900000L,
                gateType = "ENTREE",
                whatsappAlertSent = true,
                securityAgentName = "Portail Principal"
            ),
            GateCheckInEntity(
                id = 3L,
                schoolId = "gs-lasme-abidjan",
                studentId = 5,
                studentName = "Awa Diallo",
                className = "3e A",
                matricule = "MAT-2026-3EA-001",
                parentWhatsApp = "+225 07 45 82 10 01",
                checkInTime = now - 6500000L,
                gateType = "ENTREE",
                whatsappAlertSent = true,
                securityAgentName = "Portail Principal"
            )
        )
        records.forEach { dao.insertGateCheckIn(it) }
    }

    suspend fun authenticate(username: String, password: String): UserAccount? {
        val cleanUser = username.trim()
        val cleanPass = password.trim()

        // Recherche par identifiant dans la base
        var entity = dao.getUserByUsername(cleanUser)

        // Si non trouvé, tentative avec les alias de l'administrateur LASME
        if (entity == null) {
            val isLasmeUser = cleanUser.equals("LNGR@1997", ignoreCase = true) ||
                    cleanUser.equals("LASME", ignoreCase = true) ||
                    cleanUser.equals("LNGR", ignoreCase = true) ||
                    cleanUser.replace(" ", "").equals("LNGR@1997", ignoreCase = true)

            if (isLasmeUser) {
                entity = dao.getUserByUsername("LNGR@1997")
            }
        }

        if (entity == null) return null

        // Vérification mot de passe
        val passMatches = entity.password.trim() == cleanPass ||
                (entity.username.equals("LNGR@1997", ignoreCase = true) && cleanPass.equals("LASME1997", ignoreCase = true))

        if (passMatches && entity.isActive) {
            return UserAccount(
                id = entity.id,
                username = entity.username,
                password = entity.password,
                fullName = entity.fullName,
                role = try { UserRole.valueOf(entity.role) } catch (e: Exception) { UserRole.DIRECTION },
                email = entity.email,
                phone = entity.phone,
                associatedStudentId = entity.associatedStudentId,
                subjectId = entity.subjectId,
                className = entity.className,
                studentClass = entity.studentClass,
                controlledClasses = entity.controlledClasses,
                taughtSubjects = entity.taughtSubjects,
                attachedStudents = entity.attachedStudents,
                createdAt = entity.createdAt,
                isActive = entity.isActive
            )
        }
        return null
    }

    suspend fun createUserAccount(user: UserAccount): Boolean {
        val existing = dao.getUserByUsername(user.username.trim().lowercase())
        if (existing != null) return false

        dao.insertUser(
            UserAccountEntity(
                id = user.id.ifEmpty { UUID.randomUUID().toString() },
                username = user.username.trim().lowercase(),
                password = user.password.trim(),
                fullName = user.fullName.trim(),
                role = user.role.name,
                email = user.email.trim(),
                phone = user.phone.trim(),
                associatedStudentId = user.associatedStudentId,
                subjectId = user.subjectId,
                className = user.className,
                studentClass = user.studentClass,
                controlledClasses = user.controlledClasses,
                taughtSubjects = user.taughtSubjects,
                attachedStudents = user.attachedStudents,
                createdAt = user.createdAt,
                isActive = user.isActive
            )
        )
        return true
    }

    suspend fun deleteUserAccount(userId: String) {
        dao.deleteUser(userId)
    }

    suspend fun updateUserPassword(userId: String, newPass: String) {
        dao.updateUserPassword(userId, newPass.trim())
    }

    private suspend fun seedInitialData() {
        // Students
        val studentEntities = initialStudents.map { st ->
            StudentEntity(
                id = st.id,
                name = st.name,
                className = st.className,
                parentName = st.parentName,
                tuitionFee = st.tuitionFee,
                paidAmount = st.paidAmount,
                whatsappNumber = st.whatsappNumber,
                matricule = st.matricule,
                schoolId = "gs-lasme-abidjan",
                isAccessSuspended = st.isAccessSuspended,
                directorWaiver = st.directorWaiver
            )
        }
        dao.insertStudents(studentEntities)

        // Grades
        val gradeEntities = mutableListOf<GradeEntity>()
        initialStudentNames.indices.forEach { i ->
            subjects.indices.forEach { j ->
                listOf(1, 2, 3).forEach { t ->
                    val score = Math.min(20.0, 8.0 + ((i * 7 + j * 5 + t * 3) % 11) + (i % 3)).toDouble()
                    gradeEntities.add(GradeEntity(i, j, t, score))
                }
            }
        }
        dao.insertGrades(gradeEntities)

        // Attendance
        val attendanceEntities = initialStudentNames.indices.map { i ->
            val status = when (i) {
                3 -> "A"
                5 -> "R"
                else -> "P"
            }
            AttendanceEntity(i, status)
        }
        dao.insertAttendanceList(attendanceEntities)

        // Discipline
        val now = System.currentTimeMillis()
        dao.insertDiscipline(DisciplineEntity(1, 5, "Yanis Cissé", "Retards répétés au cours de 8h", now - 86400000L * 2))
        dao.insertDiscipline(DisciplineEntity(2, 7, "Noé Petit", "Utilisation du téléphone portable en classe", now - 86400000L))

        // Receipts
        dao.insertReceipt(ReceiptEntity(1, "REC-0001", 0, "Awa Diallo", 450_000L, now - 86400000L * 5, "Caisse Principale", "Virement bancaire"))
        dao.insertReceipt(ReceiptEntity(2, "REC-0002", 1, "Karim Traoré", 300_000L, now - 86400000L * 3, "Caisse Principale", "Espèces"))
        dao.insertReceipt(ReceiptEntity(3, "REC-0003", 3, "Moussa Koné", 150_000L, now - 86400000L * 1, "Caisse Principale", "Mobile Money"))

        // Notifications
        dao.insertNotification(NotificationEntity(1, null, null, "Rentrée scolaire", "Rentrée officielle lundi à 8h. Merci de régler la première tranche avant le 15.", now - 86400000L * 4, "system", false))
        dao.insertNotification(NotificationEntity(2, "PARENT", 0, "Bulletin T1 disponible", "Le bulletin du 1er trimestre d'Awa Diallo est validé et consultable.", now - 86400000L * 2, "bulletin", false))
        dao.insertNotification(NotificationEntity(3, "PARENT", 3, "Alerte Absence", "Moussa Koné a été noté absent ce matin en 3e A.", now - 3600000L * 4, "absence", false))

        // Initial Conversations & Messages (cross-role direct messaging)
        seedChatData(now)
    }

    private suspend fun seedChatData(now: Long) {
        val conv1 = ConversationEntity(
            id = "conv_dir_parent_awa",
            contactName = "Famille Diallo (Parent d'Awa)",
            contactRole = "PARENT",
            contactId = "parent_0",
            topic = "Suivi scolaire & Félicitations",
            lastMessage = "Merci Monsieur le Principal pour vos encouragements !",
            lastTimestamp = now - 1800000L,
            unreadCount = 0
        )
        val conv2 = ConversationEntity(
            id = "conv_prof_math_parent_karim",
            contactName = "M. Konaté (Mathématiques)",
            contactRole = "ENSEIGNANT",
            contactId = "prof_0",
            topic = "Devoir de synthèse & Géométrie",
            lastMessage = "Karim fait de très beaux progrès en géométrie spatiale ce trimestre.",
            lastTimestamp = now - 3600000L * 3,
            unreadCount = 1
        )
        val conv3 = ConversationEntity(
            id = "conv_caisse_parent_moussa",
            contactName = "Service Caisse & Scolarité",
            contactRole = "CAISSE",
            contactId = "caisse_main",
            topic = "Règlement 2ème tranche scolarité",
            lastMessage = "Bonjour, votre versement de 150 000 FCFA a bien été enregistré. Reçu REC-0003 disponible.",
            lastTimestamp = now - 86400000L,
            unreadCount = 0
        )
        val conv4 = ConversationEntity(
            id = "conv_dir_prof_dupont",
            contactName = "Mme Dupont (Français)",
            contactRole = "ENSEIGNANT",
            contactId = "prof_1",
            topic = "Conseil de classe 3e A",
            lastMessage = "Toutes les moyennes de Français sont saisies et validées.",
            lastTimestamp = now - 86400000L * 2,
            unreadCount = 0
        )

        dao.insertConversations(listOf(conv1, conv2, conv3, conv4))

        // Messages for conv1
        val m1 = ChatMessageEntity(
            id = "msg_1",
            conversationId = conv1.id,
            senderId = "direction",
            senderName = "Direction (M. le Principal)",
            senderRole = "DIRECTION",
            recipientId = "parent_0",
            recipientName = "Famille Diallo",
            content = "Chers parents, je tiens à vous féliciter pour l'excellence des résultats d'Awa, classée 1ère de la 3e A ce trimestre.",
            timestamp = now - 3600000L,
            status = "READ"
        )
        val m2 = ChatMessageEntity(
            id = "msg_2",
            conversationId = conv1.id,
            senderId = "parent_0",
            senderName = "Famille Diallo",
            senderRole = "PARENT",
            recipientId = "direction",
            recipientName = "Direction",
            content = "Merci Monsieur le Principal pour vos encouragements ! Nous continuons à l'accompagner avec rigueur.",
            timestamp = now - 1800000L,
            status = "READ"
        )

        // Messages for conv2
        val m3 = ChatMessageEntity(
            id = "msg_3",
            conversationId = conv2.id,
            senderId = "prof_0",
            senderName = "M. Konaté (Maths)",
            senderRole = "ENSEIGNANT",
            recipientId = "parent_1",
            recipientName = "Famille Traoré",
            content = "Bonjour, je tenais à vous informer que Karim fait de très beaux progrès en géométrie spatiale ce trimestre. Qu'il poursuive ainsi.",
            timestamp = now - 3600000L * 3,
            status = "DELIVERED"
        )

        // Messages for conv3
        val m4 = ChatMessageEntity(
            id = "msg_4",
            conversationId = conv3.id,
            senderId = "caisse_main",
            senderName = "Caisse Principale",
            senderRole = "CAISSE",
            recipientId = "parent_3",
            recipientName = "Famille Koné",
            content = "Bonjour, votre versement de 150 000 FCFA a bien été enregistré. Reçu REC-0003 disponible dans votre espace.",
            timestamp = now - 86400000L,
            status = "READ"
        )

        // Messages for conv4
        val m5 = ChatMessageEntity(
            id = "msg_5",
            conversationId = conv4.id,
            senderId = "direction",
            senderName = "Direction",
            senderRole = "DIRECTION",
            recipientId = "prof_1",
            recipientName = "Mme Dupont",
            content = "Bonjour Mme Dupont, avez-vous pu finaliser les appréciations de Français pour la 3e A ?",
            timestamp = now - 86400000L * 2 - 3600000L,
            status = "READ"
        )
        val m6 = ChatMessageEntity(
            id = "msg_6",
            conversationId = conv4.id,
            senderId = "prof_1",
            senderName = "Mme Dupont (Français)",
            senderRole = "ENSEIGNANT",
            recipientId = "direction",
            recipientName = "Direction",
            content = "Toutes les moyennes de Français sont saisies et validées.",
            timestamp = now - 86400000L * 2,
            status = "READ"
        )

        listOf(m1, m2, m3, m4, m5, m6).forEach { dao.insertChatMessage(it) }
    }

    // Actions
    suspend fun updateGrade(studentId: Int, subjectId: Int, trimester: Int, newScore: Double) {
        val clamped = newScore.coerceIn(0.0, 20.0)
        dao.insertOrUpdateGrade(GradeEntity(studentId, subjectId, trimester, clamped))
    }

    suspend fun toggleAttendance(studentId: Int, subjectName: String = "Appel en classe") {
        val all = dao.getAllAttendance().first()
        val current = all.find { it.studentId == studentId }
        val nextStatus = when (current?.status) {
            "P" -> "A"
            "A" -> "R"
            else -> "P"
        }
        dao.updateAttendance(AttendanceEntity(studentId, nextStatus))

        // Automated notification sent immediately to parent upon marking absent or late!
        if (nextStatus == "A" || nextStatus == "R") {
            val studentEntity = dao.getAllStudents().first().find { it.id == studentId }
            if (studentEntity != null) {
                val attStatus = if (nextStatus == "A") AttendanceStatus.ABSENT else AttendanceStatus.RETARD
                val student = Student(
                    studentEntity.id,
                    studentEntity.name,
                    studentEntity.className,
                    studentEntity.parentName,
                    studentEntity.tuitionFee,
                    studentEntity.paidAmount
                )
                notifyParentAbsenceAuto(student, attStatus, subjectName)
            }
        }
    }

    suspend fun notifyParentAbsenceAuto(student: Student, status: AttendanceStatus, subjectName: String = "Appel en classe") {
        val statusText = if (status == AttendanceStatus.ABSENT) "ABSENT(E)" else "EN RETARD"
        val dateFmt = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault()).format(Date())
        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = student.id,
                title = "⚠️ Alerte Vie Scolaire : ${student.name} $statusText",
                message = "Information parent : Votre enfant ${student.name} (${student.className}) a été enregistré(e) $statusText ce jour ($dateFmt) lors du cours de $subjectName. Merci de transmettre un justificatif à la vie scolaire.",
                timestamp = System.currentTimeMillis(),
                type = "absence",
                isRead = false
            )
        )
    }

    suspend fun notifyParentsOfAbsences() {
        val all = dao.getAllAttendance().first()
        val allStudents = dao.getAllStudents().first()
        all.forEach { att ->
            if (att.status != "P") {
                val studentEntity = allStudents.find { it.id == att.studentId }
                val studentName = studentEntity?.name ?: initialStudentNames.getOrElse(att.studentId) { "Élève" }
                val label = if (att.status == "A") "absent(e)" else "en retard"
                val notif = NotificationEntity(
                    targetRole = "PARENT",
                    targetStudentId = att.studentId,
                    title = "Alerte Présence : $studentName ($label)",
                    message = "Notification officielle : $studentName est noté(e) $label aujourd'hui au Groupe Scolaire Lasme. Justificatif attendu par l'éducateur.",
                    timestamp = System.currentTimeMillis(),
                    type = "absence",
                    isRead = false
                )
                dao.insertNotification(notif)
            }
        }
    }

    suspend fun addDisciplineWarning(studentId: Int, studentName: String, reason: String) {
        val item = DisciplineEntity(
            studentId = studentId,
            studentName = studentName,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        dao.insertDiscipline(item)

        // Notification to parents
        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = studentId,
                title = "Avertissement disciplinaire : $studentName",
                message = reason,
                timestamp = System.currentTimeMillis(),
                type = "absence",
                isRead = false
            )
        )
    }

    suspend fun processTuitionPayment(studentId: Int, amount: Long, method: String): String {
        val student = dao.getAllStudents().first().find { it.id == studentId } ?: return ""
        val actualAmount = amount.coerceAtMost(student.tuitionFee - student.paidAmount)
        if (actualAmount <= 0) return ""

        dao.addPayment(studentId, actualAmount)

        val receiptsCount = dao.getAllReceipts().first().size + 1
        val receiptNumber = "REC-" + String.format("%04d", receiptsCount)
        val receipt = ReceiptEntity(
            receiptNumber = receiptNumber,
            studentId = studentId,
            studentName = student.name,
            amount = actualAmount,
            timestamp = System.currentTimeMillis(),
            cashierName = "Caisse Principale",
            paymentMethod = method
        )
        dao.insertReceipt(receipt)

        // Automatic unblocking if suspended
        if (student.isAccessSuspended) {
            dao.updateStudentAccessSuspension(studentId, false)
            dao.insertNotification(
                NotificationEntity(
                    targetRole = "PARENT",
                    targetStudentId = studentId,
                    title = "Accès Débloqué Automatiquement",
                    message = "Suite à votre règlement ($method), l'accès aux cours en ligne et devoirs PDF de ${student.name} est automatiquement réactivé.",
                    timestamp = System.currentTimeMillis(),
                    type = "access_unlocked",
                    isRead = false
                )
            )
        }

        // Notification
        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = studentId,
                title = "Paiement validé ($receiptNumber)",
                message = "Règlement de ${formatAmount(actualAmount)} FCFA pour ${student.name}. Reçu disponible.",
                timestamp = System.currentTimeMillis(),
                type = "finance",
                isRead = false
            )
        )
        return receiptNumber
    }

    suspend fun publishBulletins(trimester: Int) {
        dao.insertNotification(
            NotificationEntity(
                targetRole = null,
                targetStudentId = null,
                title = "Bulletins du Trimestre $trimester publiés",
                message = "Les bulletins officiels du trimestre $trimester sont validés par la Direction et consultables en ligne.",
                timestamp = System.currentTimeMillis(),
                type = "bulletin",
                isRead = false
            )
        )
    }

    suspend fun broadcastDirectionMessage(target: String, messageText: String) {
        val targetRole = when (target) {
            "Parents" -> "PARENT"
            "Enseignants" -> "ENSEIGNANT"
            else -> null
        }
        dao.insertNotification(
            NotificationEntity(
                targetRole = targetRole,
                targetStudentId = null,
                title = "Message de la Direction ($target)",
                message = messageText,
                timestamp = System.currentTimeMillis(),
                type = "message",
                isRead = false
            )
        )
    }

    // Timetable Slots Management (Éducateur & Direction)
    suspend fun saveTimetableSlot(slot: TimetableSlot): Long {
        val entity = TimetableSlotEntity(
            id = slot.id,
            className = slot.className,
            day = slot.day,
            startTime = slot.startTime,
            endTime = slot.endTime,
            subjectName = slot.subjectName,
            teacherName = slot.teacherName,
            classroom = slot.classroom
        )
        return dao.insertTimetableSlot(entity)
    }

    suspend fun deleteTimetableSlot(slotId: Long) {
        dao.deleteTimetableSlot(slotId)
    }

    // Homework Exercises Management (Professeur & Consultation)
    suspend fun saveHomeworkExercise(exercise: HomeworkExercise): Long {
        val entity = HomeworkExerciseEntity(
            id = exercise.id,
            title = exercise.title,
            subjectName = exercise.subjectName,
            className = exercise.className,
            teacherName = exercise.teacherName,
            dueDate = exercise.dueDate,
            description = exercise.description,
            pdfFileName = exercise.pdfFileName,
            pdfFileSize = exercise.pdfFileSize,
            timestamp = exercise.timestamp
        )
        val id = dao.insertHomeworkExercise(entity)

        // Broadcast notification to students and parents of that class
        dao.insertNotification(
            NotificationEntity(
                targetRole = null,
                targetStudentId = null,
                title = "Nouveau devoir PDF (${exercise.className})",
                message = "${exercise.teacherName} a déposé un devoir en ${exercise.subjectName} : « ${exercise.title} ». Document PDF disponible au téléchargement. Échéance : ${exercise.dueDate}.",
                timestamp = System.currentTimeMillis(),
                type = "bulletin",
                isRead = false
            )
        )
        return id
    }

    suspend fun deleteHomeworkExercise(exerciseId: Long) {
        dao.deleteHomeworkExercise(exerciseId)
    }

    // Instant Mobile Payment (Wave & Orange Money)
    suspend fun processInstantPayment(
        studentId: Int,
        amount: Long,
        provider: String, // "Wave" or "Orange Money"
        payerPhone: String
    ): PaymentReceiptItem {
        val allStudents = dao.getAllStudents().first()
        val student = allStudents.find { it.id == studentId }
            ?: StudentEntity(studentId, "Élève", "3e A", "Famille", 450_000L, 0L)
        val receiptNumber = "REC-${provider.take(2).uppercase()}-${System.currentTimeMillis() % 1000000}"

        dao.addPayment(studentId, amount)
        val receiptEntity = ReceiptEntity(
            receiptNumber = receiptNumber,
            studentId = studentId,
            studentName = student.name,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            cashierName = "Paiement Instantané $provider",
            paymentMethod = provider
        )
        dao.insertReceipt(receiptEntity)

        // Notification to Parent
        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = studentId,
                title = "Paiement instantané $provider validé",
                message = "Votre versement de ${formatAmount(amount)} FCFA pour ${student.name} via $provider a été validé avec succès (Réf : $receiptNumber). Reçu officiel émis.",
                timestamp = System.currentTimeMillis(),
                type = "finance",
                isRead = false
            )
        )

        // Notification to Comptabilité / Caisse
        dao.insertNotification(
            NotificationEntity(
                targetRole = "CAISSE",
                targetStudentId = studentId,
                title = "Règlement instantané $provider reçu",
                message = "Encaissement immédiat de ${formatAmount(amount)} FCFA reçu via $provider ($payerPhone) pour l'élève ${student.name} (Réf : $receiptNumber). Trésorerie mise à jour.",
                timestamp = System.currentTimeMillis(),
                type = "finance",
                isRead = false
            )
        )

        return PaymentReceiptItem(
            id = receiptEntity.id,
            receiptNumber = receiptNumber,
            studentId = studentId,
            studentName = student.name,
            amount = amount,
            timestamp = receiptEntity.timestamp,
            cashierName = receiptEntity.cashierName,
            paymentMethod = receiptEntity.paymentMethod
        )
    }

    // Instant Messaging methods
    suspend fun sendChatMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderRole: UserRole,
        recipientId: String,
        recipientName: String,
        content: String
    ): ChatMessage {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val entity = ChatMessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole.name,
            recipientId = recipientId,
            recipientName = recipientName,
            content = content,
            timestamp = timestamp,
            status = "SENT"
        )
        dao.insertChatMessage(entity)
        dao.updateConversationLastMessage(conversationId, content, timestamp)

        val chatMessage = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            recipientId = recipientId,
            recipientName = recipientName,
            content = content,
            timestamp = timestamp,
            status = MessageStatus.SENT
        )

        // Real-time synchronization to Cloud Firestore
        firestoreService.sendMessage(chatMessage)

        return chatMessage
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus, conversationId: String? = null) {
        dao.updateMessageStatus(messageId, status.name)
        if (conversationId != null) {
            firestoreService.updateMessageStatus(conversationId, messageId, status)
        }
    }

    suspend fun markConversationAsRead(convId: String, currentRole: UserRole) {
        dao.markMessagesAsRead(convId, "READ", currentRole.name)
        dao.clearConversationUnread(convId)
    }

    suspend fun createOrGetConversation(
        contactName: String,
        contactRole: UserRole,
        contactId: String,
        topic: String
    ): String {
        val existing = dao.getAllConversations().first()
        val found = existing.find { it.contactId == contactId }
        if (found != null) {
            return found.id
        }

        val newId = "conv_" + UUID.randomUUID().toString().take(8)
        val entity = ConversationEntity(
            id = newId,
            contactName = contactName,
            contactRole = contactRole.name,
            contactId = contactId,
            topic = topic,
            lastMessage = "Nouvelle conversation démarrée",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        dao.insertConversation(entity)
        return newId
    }

    // Online Courses & WhatsApp Broadcast
    suspend fun startOnlineCourseSession(
        className: String,
        subjectName: String,
        teacherName: String,
        topic: String,
        meetingUrl: String
    ): OnlineCourseSession {
        val allStudents = dao.getAllStudents().first()
        val classStudents = allStudents.filter { it.className.equals(className, ignoreCase = true) }
        val targetCount = classStudents.size
        val whatsappCount = classStudents.count { it.whatsappNumber.isNotBlank() }

        val entity = OnlineCourseEntity(
            id = 0L,
            className = className,
            subjectName = subjectName,
            teacherName = teacherName,
            topic = topic,
            meetingUrl = meetingUrl,
            startedAt = System.currentTimeMillis(),
            isActive = true,
            targetStudentsCount = targetCount,
            whatsappSentCount = whatsappCount
        )
        val courseId = dao.insertOnlineCourse(entity)

        val session = OnlineCourseSession(
            id = courseId,
            className = className,
            subjectName = subjectName,
            teacherName = teacherName,
            topic = topic,
            meetingUrl = meetingUrl,
            startedAt = entity.startedAt,
            isActive = true,
            targetStudentsCount = targetCount,
            whatsappSentCount = whatsappCount
        )

        // 1. Automated in-app notification to all students and parents of that class
        classStudents.forEach { st ->
            dao.insertNotification(
                NotificationEntity(
                    id = 0L,
                    targetRole = null,
                    targetStudentId = st.id,
                    title = "🔴 Cours en direct : $subjectName ($className)",
                    message = "$teacherName a démarré le cours en ligne « $topic ». Le lien vidéo a été diffusé sur votre WhatsApp (${st.whatsappNumber}). Cliquez pour rejoindre.",
                    timestamp = System.currentTimeMillis(),
                    type = "video_course",
                    isRead = false
                )
            )
        }

        // 2. Automated general broadcast notification
        dao.insertNotification(
            NotificationEntity(
                id = 0L,
                targetRole = "ELEVE",
                targetStudentId = null,
                title = "🔴 Cours en direct en $className",
                message = "Session vidéo de $subjectName en cours avec $teacherName. Lien vidéo diffusé à $whatsappCount élèves sur WhatsApp.",
                timestamp = System.currentTimeMillis(),
                type = "video_course",
                isRead = false
            )
        )

        return session
    }

    suspend fun closeOnlineCourse(courseId: Long) {
        dao.closeOnlineCourse(courseId)
    }

    suspend fun updateStudentWhatsApp(studentId: Int, whatsapp: String, className: String? = null) {
        if (className != null && className.isNotBlank()) {
            dao.updateStudentClassAndWhatsApp(studentId, className.trim(), whatsapp.trim())
        } else {
            dao.updateStudentWhatsApp(studentId, whatsapp.trim())
        }
    }

    suspend fun registerStudent(
        name: String,
        className: String,
        parentName: String,
        whatsappNumber: String,
        tuitionFee: Long = 450_000L
    ): Student {
        val all = dao.getAllStudents().first()
        val newId = (all.maxOfOrNull { it.id } ?: 0) + 1
        val cleanClass = className.trim().uppercase().replace(" ", "")
        val autoMatricule = "MAT-2026-$cleanClass-${String.format("%04d", newId)}"
        val entity = StudentEntity(
            id = newId,
            name = name.trim(),
            className = className.trim(),
            parentName = parentName.trim(),
            tuitionFee = tuitionFee,
            paidAmount = 0L,
            whatsappNumber = whatsappNumber.trim(),
            matricule = autoMatricule,
            isAccessSuspended = false,
            directorWaiver = false
        )
        dao.insertStudent(entity)
        return Student(
            id = entity.id,
            name = entity.name,
            className = entity.className,
            parentName = entity.parentName,
            tuitionFee = entity.tuitionFee,
            paidAmount = entity.paidAmount,
            whatsappNumber = entity.whatsappNumber,
            matricule = autoMatricule,
            isAccessSuspended = false,
            directorWaiver = false
        )
    }

    // Access Control & Tuition Debt Recovery
    suspend fun setStudentAccessSuspension(studentId: Int, isSuspended: Boolean) {
        dao.updateStudentAccessSuspension(studentId, isSuspended)
        val student = dao.getAllStudents().first().find { it.id == studentId }
        val stName = student?.name ?: "l'élève"
        if (isSuspended) {
            dao.insertNotification(
                NotificationEntity(
                    targetRole = "PARENT",
                    targetStudentId = studentId,
                    title = "Accès suspendu pour impayé",
                    message = "L'accès aux cours en ligne et devoirs PDF de $stName a été restreint par la Comptabilité. Veuillez régulariser via Wave/Orange Money.",
                    timestamp = System.currentTimeMillis(),
                    type = "access_restricted",
                    isRead = false
                )
            )
        } else {
            dao.insertNotification(
                NotificationEntity(
                    targetRole = "PARENT",
                    targetStudentId = studentId,
                    title = "Accès rétabli",
                    message = "L'accès aux cours en ligne et devoirs PDF de $stName a été rétabli par le service comptabilité.",
                    timestamp = System.currentTimeMillis(),
                    type = "access_restored",
                    isRead = false
                )
            )
        }
    }

    suspend fun setDirectorWaiver(studentId: Int, waiver: Boolean) {
        dao.updateStudentDirectorWaiver(studentId, waiver)
        val student = dao.getAllStudents().first().find { it.id == studentId }
        val stName = student?.name ?: "l'élève"
        if (waiver) {
            dao.insertNotification(
                NotificationEntity(
                    targetRole = "PARENT",
                    targetStudentId = studentId,
                    title = "Dérogation accordée par le Directeur",
                    message = "Le Directeur a accordé une dérogation spéciale à $stName. Les cours en ligne et devoirs PDF sont accessibles sans restriction.",
                    timestamp = System.currentTimeMillis(),
                    type = "director_waiver",
                    isRead = false
                )
            )
        }
    }

    suspend fun sendTuitionReminder(studentId: Int): String {
        val student = dao.getAllStudents().first().find { it.id == studentId } ?: return ""
        val remaining = (student.tuitionFee - student.paidAmount).coerceAtLeast(0L)
        val payLink = "https://pay.edugest.ci/lasme?studentId=$studentId&amount=$remaining"
        val message = "GROUPE SCOLAIRE LASME : Rappel scolarité pour ${student.name}. Reste à payer : ${formatAmount(remaining)} FCFA. Réglez en 1 clic par Wave/Orange/MTN/Moov : $payLink"

        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = studentId,
                title = "Rappel d'échéance de scolarité",
                message = "Relance envoyée au parent (${student.parentName} - ${student.whatsappNumber}) : Reste à solder ${formatAmount(remaining)} FCFA avec lien de paiement Mobile Money.",
                timestamp = System.currentTimeMillis(),
                type = "tuition_reminder",
                isRead = false
            )
        )
        return message
    }

    // Cahier de texte (Textbook)
    suspend fun addTextbookEntry(
        className: String,
        subjectName: String,
        teacherName: String,
        chapterTitle: String,
        summary: String,
        homework: String,
        progress: Int
    ): Long {
        val entry = TextbookEntryEntity(
            className = className.trim(),
            subjectName = subjectName.trim(),
            teacherName = teacherName.trim(),
            sessionDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(Date()),
            chapterTitle = chapterTitle.trim(),
            objectivesAndSummary = summary.trim(),
            homeworkAssigned = homework.trim(),
            progressPercentage = progress.coerceIn(0, 100),
            isDirectorValidated = false,
            directorComment = "",
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertTextbookEntry(entry)
        dao.insertNotification(
            NotificationEntity(
                targetRole = "DIRECTION",
                targetStudentId = null,
                title = "Nouveau chapitre saisi au cahier de texte",
                message = "$teacherName a renseigné « $chapterTitle » en $subjectName ($className). Avancement : $progress%.",
                timestamp = System.currentTimeMillis(),
                type = "textbook",
                isRead = false
            )
        )
        return id
    }

    suspend fun validateTextbookEntry(id: Long, comment: String) {
        dao.validateTextbookEntry(id, comment)
    }

    // Gate CheckIn (Pointage Badge QR au portail)
    suspend fun recordGateCheckIn(studentId: Int, gateType: String = "ENTREE"): GateCheckInRecord? {
        val student = dao.getAllStudents().first().find { it.id == studentId } ?: return null
        val now = System.currentTimeMillis()
        val record = GateCheckInEntity(
            studentId = student.id,
            studentName = student.name,
            className = student.className,
            matricule = student.matricule.ifBlank { "MAT-2026-${student.id}" },
            parentWhatsApp = student.whatsappNumber,
            checkInTime = now,
            gateType = gateType,
            whatsappAlertSent = student.whatsappNumber.isNotBlank(),
            securityAgentName = "Portail Principal"
        )
        val newId = dao.insertGateCheckIn(record)

        val timeStr = SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(now))
        val actionVerb = if (gateType == "ENTREE") "est entré(e) dans l'établissement" else "a quitté l'établissement"

        dao.insertNotification(
            NotificationEntity(
                targetRole = "PARENT",
                targetStudentId = student.id,
                title = "Pointage portail ($gateType)",
                message = "${student.name} $actionVerb à $timeStr. Alerte instantanée WhatsApp envoyée au ${student.whatsappNumber}.",
                timestamp = now,
                type = "gate_alert",
                isRead = false
            )
        )

        return GateCheckInRecord(
            id = newId,
            studentId = record.studentId,
            studentName = record.studentName,
            className = record.className,
            matricule = record.matricule,
            parentWhatsApp = record.parentWhatsApp,
            checkInTime = record.checkInTime,
            gateType = record.gateType,
            whatsappAlertSent = record.whatsappAlertSent,
            securityAgentName = record.securityAgentName
        )
    }

    // Multi-tenant School Management
    suspend fun createSchoolTenant(
        name: String,
        city: String,
        code: String,
        phone: String,
        waveId: String,
        orangeId: String
    ) {
        val tenantId = "school-${code.lowercase().replace(" ", "-")}-${System.currentTimeMillis() % 10000}"
        val entity = SchoolTenantEntity(
            id = tenantId,
            name = name.trim(),
            code = code.trim().uppercase(),
            city = city.trim(),
            phone = phone.trim(),
            waveMerchantId = waveId.trim(),
            orangeMerchantId = orangeId.trim(),
            studentsCount = 0,
            teachersCount = 0
        )
        dao.insertSchoolTenant(entity)
    }

    suspend fun duplicateSchoolTenant(
        sourceTenantId: String,
        newName: String,
        newCode: String,
        newCity: String
    ) {
        val existing = dao.getAllSchoolTenants().first().find { it.id == sourceTenantId }
        val newId = "school-${newCode.lowercase().replace(" ", "-")}-${System.currentTimeMillis() % 10000}"
        val duplicated = SchoolTenantEntity(
            id = newId,
            name = newName.trim(),
            code = newCode.trim().uppercase(),
            city = newCity.trim(),
            phone = existing?.phone ?: "+225 01 00 00 00",
            waveMerchantId = "WAVE-${newCode.trim().uppercase()}",
            orangeMerchantId = "OM-${newCode.trim().uppercase()}",
            studentsCount = 0,
            teachersCount = existing?.teachersCount ?: 5
        )
        dao.insertSchoolTenant(duplicated)
    }

    companion object {
        data class InitialStudentInfo(
            val id: Int,
            val name: String,
            val className: String,
            val parentName: String,
            val tuitionFee: Long,
            val paidAmount: Long,
            val whatsappNumber: String,
            val matricule: String,
            val isAccessSuspended: Boolean = false,
            val directorWaiver: Boolean = false
        )

        val initialStudents = listOf(
            // Classe de 6e 1 (Classe mise en avant pour le cours en ligne via WhatsApp)
            InitialStudentInfo(0, "Emmanuel Lasme", "6e 1", "M. Roland Lasme", 450_000L, 450_000L, "+225 07 89 45 12 30", "MAT-2026-6E1-001", false, false),
            InitialStudentInfo(1, "Aminata Coulibaly", "6e 1", "Famille Coulibaly", 450_000L, 300_000L, "+225 05 66 77 88 99", "MAT-2026-6E1-002", false, false),
            InitialStudentInfo(2, "Mohamed Sanogo", "6e 1", "M. Ibrahim Sanogo", 450_000L, 450_000L, "+225 01 23 45 67 89", "MAT-2026-6E1-003", false, false),
            InitialStudentInfo(3, "Sarah Bamba", "6e 1", "Mme Fatoumata Bamba", 450_000L, 150_000L, "+225 07 55 44 33 22", "MAT-2026-6E1-004", true, false), // Impayé -> suspendu par comptable
            InitialStudentInfo(4, "David Kouadio", "6e 1", "Famille Kouadio", 450_000L, 225_000L, "+225 05 12 34 56 78", "MAT-2026-6E1-005", false, false),
            // Classe de 3e A
            InitialStudentInfo(5, "Awa Diallo", "3e A", "Famille Diallo", 450_000L, 450_000L, "+225 07 45 82 10 01", "MAT-2026-3EA-001", false, false),
            InitialStudentInfo(6, "Karim Traoré", "3e A", "Famille Traoré", 450_000L, 300_000L, "+225 05 52 33 44 12", "MAT-2026-3EA-002", false, false),
            InitialStudentInfo(7, "Léa Martin", "3e A", "M. Pierre Martin", 450_000L, 450_000L, "+225 07 98 76 54 32", "MAT-2026-3EA-003", false, false),
            InitialStudentInfo(8, "Moussa Koné", "3e A", "Famille Koné", 450_000L, 150_000L, "+225 01 77 88 99 23", "MAT-2026-3EA-004", true, true), // Dérogation Directeur
            InitialStudentInfo(9, "Inès Bernard", "3e A", "Famille Bernard", 450_000L, 0L, "+225 05 90 12 34 56", "MAT-2026-3EA-005", true, false), // Impayé total
            InitialStudentInfo(10, "Yanis Cissé", "3e A", "M. Cissé", 450_000L, 300_000L, "+225 07 33 44 55 66", "MAT-2026-3EA-006", false, false),
            InitialStudentInfo(11, "Fatou Sy", "3e A", "Famille Sy", 450_000L, 450_000L, "+225 07 11 22 33 44", "MAT-2026-3EA-007", false, false),
            InitialStudentInfo(12, "Noé Petit", "3e A", "Mme Petit", 450_000L, 225_000L, "+225 05 88 99 00 11", "MAT-2026-3EA-008", false, false)
        )

        val initialStudentNames = initialStudents.map { it.name }
        val initialPaidAmounts = initialStudents.map { it.paidAmount }

        fun formatAmount(amount: Long): String {
            return String.format("%,d", amount).replace(',', ' ')
        }
    }
}
