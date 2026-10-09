package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EduGestDatabase
import com.example.data.model.*
import com.example.data.repository.EduGestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EduGestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EduGestRepository = EduGestRepository(
        EduGestDatabase.getDatabase(application).dao(),
        viewModelScope
    )

    // Current State
    private val _authenticatedUser = MutableStateFlow<UserAccount?>(null)
    val authenticatedUser: StateFlow<UserAccount?> = _authenticatedUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.DIRECTION)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedStudentId = MutableStateFlow(0)
    val selectedStudentId: StateFlow<Int> = _selectedStudentId.asStateFlow()

    private val _selectedTrimester = MutableStateFlow(1)
    val selectedTrimester: StateFlow<Int> = _selectedTrimester.asStateFlow()

    private val _selectedSubjectIndex = MutableStateFlow(0)
    val selectedSubjectIndex: StateFlow<Int> = _selectedSubjectIndex.asStateFlow()

    // Instant Chat States
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _isContactTyping = MutableStateFlow(false)
    val isContactTyping: StateFlow<Boolean> = _isContactTyping.asStateFlow()

    private val _inAppNotification = MutableStateFlow<NotificationEntry?>(null)
    val inAppNotification: StateFlow<NotificationEntry?> = _inAppNotification.asStateFlow()

    // Data streams from repository
    val students: StateFlow<List<Student>> = repository.students
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grades: StateFlow<List<GradeEntry>> = repository.grades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<StudentAttendance>> = repository.attendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discipline: StateFlow<List<DisciplineItem>> = repository.discipline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receipts: StateFlow<List<PaymentReceiptItem>> = repository.receipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntry>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<Conversation>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserAccount>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableSlots: StateFlow<List<TimetableSlot>> = repository.timetableSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeworkExercises: StateFlow<List<HomeworkExercise>> = repository.homeworkExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlineCourses: StateFlow<List<OnlineCourseSession>> = repository.onlineCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val textbookEntries: StateFlow<List<TextbookEntry>> = repository.textbookEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gateCheckIns: StateFlow<List<GateCheckInRecord>> = repository.gateCheckIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolTenants: StateFlow<List<SchoolTenant>> = repository.schoolTenants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedClassForTimetable = MutableStateFlow("3e A")
    val selectedClassForTimetable: StateFlow<String> = _selectedClassForTimetable.asStateFlow()

    fun setSelectedClassForTimetable(className: String) {
        _selectedClassForTimetable.value = className
    }

    val subjects: List<Subject> = repository.subjects

    // Active conversation messages
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) {
                repository.getMessagesForConversation(convId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations
    fun getGrade(studentId: Int, subjectId: Int, trimester: Int): Double {
        val entry = grades.value.find {
            it.studentId == studentId && it.subjectId == subjectId && it.trimester == trimester
        }
        return entry?.score ?: 12.0
    }

    fun getStudentTrimesterAverage(studentId: Int, trimester: Int): Double {
        val totalCoef = subjects.sumOf { it.coef }
        if (totalCoef == 0) return 0.0
        val weightedSum = subjects.sumOf { subj ->
            subj.coef * getGrade(studentId, subj.id, trimester)
        }
        return (weightedSum / totalCoef)
    }

    fun getSubjectClassAverage(subjectId: Int, trimester: Int): Double {
        val stList = students.value
        if (stList.isEmpty()) return 0.0
        val sum = stList.sumOf { st -> getGrade(st.id, subjectId, trimester) }
        return sum / stList.size
    }

    fun getOverallClassAverage(trimester: Int): Double {
        val stList = students.value
        if (stList.isEmpty()) return 0.0
        val sum = stList.sumOf { st -> getStudentTrimesterAverage(st.id, trimester) }
        return sum / stList.size
    }

    fun getStudentReportCard(studentId: Int, trimester: Int): StudentReportCard? {
        val st = students.value.find { it.id == studentId } ?: return null
        val stList = students.value
        val subjectGrades = subjects.map { subj ->
            val score = getGrade(studentId, subj.id, trimester)
            val classAvg = getSubjectClassAverage(subj.id, trimester)
            SubjectGradeDetail(subj, score, classAvg)
        }

        val overallAvg = getStudentTrimesterAverage(studentId, trimester)

        // Rank
        val sortedAverages = stList.map { it.id to getStudentTrimesterAverage(it.id, trimester) }
            .sortedByDescending { it.second }
        val rank = sortedAverages.indexOfFirst { it.first == studentId } + 1

        val mention = when {
            overallAvg >= 16.0 -> "Très Bien (Félicitations du Conseil)"
            overallAvg >= 14.0 -> "Bien (Compliments)"
            overallAvg >= 12.0 -> "Assez Bien (Encouragements)"
            overallAvg >= 10.0 -> "Passable"
            else -> "Insuffisant (Avertissement de travail)"
        }

        val y1 = getStudentTrimesterAverage(studentId, 1)
        val y2 = getStudentTrimesterAverage(studentId, 2)
        val y3 = getStudentTrimesterAverage(studentId, 3)
        val annualAvg = (y1 + y2 + y3) / 3.0

        return StudentReportCard(
            student = st,
            trimester = trimester,
            subjectGrades = subjectGrades,
            overallAverage = overallAvg,
            rank = rank,
            totalStudents = stList.size,
            mention = mention,
            yearlyAverages = listOf(y1, y2, y3),
            annualAverage = annualAvg
        )
    }

    // Role & Tab Navigation
    fun setRole(role: UserRole) {
        _currentRole.value = role
        _currentTab.value = 0
        _activeConversationId.value = null
    }

    fun setTab(index: Int) {
        _currentTab.value = index
        _activeConversationId.value = null
    }

    fun setSelectedStudentId(id: Int) {
        _selectedStudentId.value = id
    }

    fun setSelectedTrimester(t: Int) {
        _selectedTrimester.value = t
    }

    fun setSelectedSubjectIndex(idx: Int) {
        _selectedSubjectIndex.value = idx
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            repository.markConversationAsRead(conversationId, _currentRole.value)
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
        _isContactTyping.value = false
    }

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    // Authentication & Access Control
    fun login(username: String, password: String) {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.isBlank() || cleanPass.isBlank()) {
            _loginError.value = "Veuillez renseigner votre identifiant et votre mot de passe."
            return
        }
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginError.value = null
            var account = repository.authenticate(cleanUser, cleanPass)

            // Secours immédiat si l'accès saisi correspond à l'administrateur LASME (LNGR@1997 ou LASME ou LNGR)
            val isLasmeInput = cleanUser.equals("LNGR@1997", ignoreCase = true) ||
                    cleanUser.equals("LASME", ignoreCase = true) ||
                    cleanUser.equals("LNGR", ignoreCase = true) ||
                    cleanUser.replace(" ", "").equals("LNGR@1997", ignoreCase = true)

            val isLasmePass = cleanPass.equals("LASME1997", ignoreCase = true)

            if (account == null && isLasmeInput && isLasmePass) {
                val adminAccount = UserAccount(
                    id = "usr_admin_lasme",
                    username = "LNGR@1997",
                    password = "LASME1997",
                    fullName = "LASME",
                    role = UserRole.DIRECTION,
                    email = "direction@gs-lasme.ci",
                    phone = "+225 01 02 03 04",
                    className = "3e A",
                    studentClass = "",
                    controlledClasses = "Toutes les classes (6e à Tle)",
                    taughtSubjects = "Direction générale"
                )
                repository.createUserAccount(adminAccount)
                account = adminAccount
            }

            _isAuthenticating.value = false
            if (account != null) {
                applyAuthenticatedUser(account)
            } else {
                _loginError.value = "Identifiant ou mot de passe incorrect. Pour LASME : Identifiant = LNGR@1997, Mot de passe = LASME1997."
            }
        }
    }

    fun quickLogin(role: UserRole) {
        viewModelScope.launch {
            val userList = users.value
            val match = userList.find { it.role == role }
            if (match != null) {
                applyAuthenticatedUser(match)
            } else {
                val admin = userList.find { it.username == "LNGR@1997" }
                    ?: UserAccount("usr_admin_lasme", "LNGR@1997", "LASME1997", "LASME", UserRole.DIRECTION)
                applyAuthenticatedUser(admin)
            }
        }
    }

    fun onGoogleSignIn(name: String, email: String?) {
        val account = UserAccount(
            id = "usr_google_${System.currentTimeMillis()}",
            username = email ?: "google_user",
            password = "",
            fullName = name,
            role = UserRole.DIRECTION
        )
        applyAuthenticatedUser(account)
    }

    private fun applyAuthenticatedUser(account: UserAccount) {
        _authenticatedUser.value = account
        _currentRole.value = account.role
        _currentTab.value = 0
        _activeConversationId.value = null
        _loginError.value = null
        if (account.associatedStudentId != null) {
            _selectedStudentId.value = account.associatedStudentId
        }
        if (account.subjectId != null) {
            _selectedSubjectIndex.value = account.subjectId
        }
        showBanner(
            title = "Bienvenue ${account.fullName}",
            message = "Connecté(e) avec succès • Espace ${account.role.badgeTitle}",
            type = "system"
        )
    }

    fun logout() {
        _authenticatedUser.value = null
        _loginError.value = null
        _activeConversationId.value = null
        _currentTab.value = 0
    }

    fun createUserAccount(
        fullName: String,
        username: String,
        password: String,
        role: UserRole,
        email: String = "",
        phone: String = "",
        associatedStudentId: Int? = null,
        subjectId: Int? = null,
        studentClass: String = "3e A",
        controlledClasses: String = "3e A",
        taughtSubjects: String = "Général",
        attachedStudents: String = "Élève 3e A"
    ) {
        viewModelScope.launch {
            val newAcc = UserAccount(
                id = "usr_" + System.currentTimeMillis().toString().takeLast(6),
                username = username.trim().lowercase(),
                password = password.trim(),
                fullName = fullName.trim(),
                role = role,
                email = email.trim(),
                phone = phone.trim(),
                associatedStudentId = associatedStudentId,
                subjectId = subjectId,
                className = studentClass.ifEmpty { "3e A" },
                studentClass = studentClass,
                controlledClasses = controlledClasses,
                taughtSubjects = taughtSubjects,
                attachedStudents = attachedStudents,
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
            val success = repository.createUserAccount(newAcc)
            if (success) {
                showBanner(
                    title = "Compte créé avec succès",
                    message = "L'accès pour ${newAcc.fullName} (${newAcc.role.displayName}) est actif avec l'identifiant ${newAcc.username}.",
                    type = "system"
                )
            } else {
                showBanner(
                    title = "Erreur de création",
                    message = "L'identifiant '${username}' est déjà utilisé par un autre compte.",
                    type = "absence"
                )
            }
        }
    }

    fun deleteUserAccount(userId: String) {
        viewModelScope.launch {
            repository.deleteUserAccount(userId)
            showBanner(
                title = "Accès révoqué",
                message = "Le compte utilisateur a été supprimé de la base.",
                type = "system"
            )
        }
    }

    fun updateUserPassword(userId: String, newPass: String) {
        viewModelScope.launch {
            repository.updateUserPassword(userId, newPass)
            showBanner(
                title = "Code d'accès mis à jour",
                message = "Le nouveau mot de passe a bien été enregistré.",
                type = "system"
            )
        }
    }

    // Timetable Management (Éducateur & Direction)
    fun saveTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.saveTimetableSlot(slot)
            showBanner(
                title = "Emploi du temps mis à jour",
                message = "Cours de ${slot.subjectName} (${slot.day} ${slot.startTime}-${slot.endTime}) enregistré pour la classe ${slot.className}.",
                type = "system"
            )
        }
    }

    fun deleteTimetableSlot(slotId: Long) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slotId)
            showBanner(
                title = "Créneau supprimé",
                message = "Le cours a été retiré de l'emploi du temps.",
                type = "system"
            )
        }
    }

    // Homework Exercises with PDF (Professeur & Consultation)
    fun saveHomeworkExercise(
        title: String,
        subjectName: String,
        className: String,
        teacherName: String,
        dueDate: String,
        description: String,
        pdfFileName: String,
        pdfFileSize: String = "1.5 Mo"
    ) {
        viewModelScope.launch {
            val exercise = HomeworkExercise(
                title = title,
                subjectName = subjectName,
                className = className,
                teacherName = teacherName,
                dueDate = dueDate,
                description = description,
                pdfFileName = pdfFileName,
                pdfFileSize = pdfFileSize,
                timestamp = System.currentTimeMillis()
            )
            repository.saveHomeworkExercise(exercise)
            showBanner(
                title = "Devoir PDF publié",
                message = "L'exercice « $title » a été publié pour la classe $className.",
                type = "bulletin"
            )
        }
    }

    fun deleteHomeworkExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteHomeworkExercise(exerciseId)
            showBanner(
                title = "Devoir retiré",
                message = "L'exercice et son document PDF ont été supprimés.",
                type = "system"
            )
        }
    }

    // Instant Payment (Wave & Orange Money)
    fun processInstantPayment(
        studentId: Int,
        amount: Long,
        provider: String, // "Wave" or "Orange Money"
        payerPhone: String
    ) {
        viewModelScope.launch {
            val receipt = repository.processInstantPayment(studentId, amount, provider, payerPhone)
            showBanner(
                title = "Paiement instantané $provider validé !",
                message = "Reçu ${receipt.receiptNumber} émis (${EduGestRepository.formatAmount(amount)} FCFA). Comptabilité synchronisée.",
                type = "finance"
            )
        }
    }

    // Online Courses via WhatsApp
    fun startOnlineCourseSession(
        className: String,
        subjectName: String,
        teacherName: String,
        topic: String,
        meetingUrl: String,
        onLaunched: ((OnlineCourseSession) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val session = repository.startOnlineCourseSession(
                className = className,
                subjectName = subjectName,
                teacherName = teacherName,
                topic = topic,
                meetingUrl = meetingUrl
            )
            showBanner(
                title = "🔴 Cours en direct démarré ($className)",
                message = "Lien vidéo diffusé instantanément sur WhatsApp aux ${session.whatsappSentCount} élèves de la classe.",
                type = "video_course"
            )
            onLaunched?.invoke(session)
        }
    }

    fun closeOnlineCourse(courseId: Long) {
        viewModelScope.launch {
            repository.closeOnlineCourse(courseId)
            showBanner(
                title = "Session en direct clôturée",
                message = "Le cours en ligne a été clôturé et archivé.",
                type = "system"
            )
        }
    }

    fun updateStudentWhatsApp(studentId: Int, whatsapp: String, className: String? = null) {
        viewModelScope.launch {
            repository.updateStudentWhatsApp(studentId, whatsapp, className)
            showBanner(
                title = "Profil élève mis à jour",
                message = "Le numéro WhatsApp de l'élève a été enregistré pour sa classe.",
                type = "system"
            )
        }
    }

    fun registerStudent(
        name: String,
        className: String,
        parentName: String,
        whatsappNumber: String,
        tuitionFee: Long = 450_000L
    ) {
        viewModelScope.launch {
            val st = repository.registerStudent(name, className, parentName, whatsappNumber, tuitionFee)
            showBanner(
                title = "Élève enregistré",
                message = "${st.name} rattaché(e) à la classe ${st.className} (WhatsApp: ${st.whatsappNumber}).",
                type = "system"
            )
        }
    }

    // Actions
    fun updateGrade(studentId: Int, subjectId: Int, trimester: Int, newScore: Double) {
        viewModelScope.launch {
            repository.updateGrade(studentId, subjectId, trimester, newScore)
        }
    }

    fun toggleAttendance(studentId: Int) {
        viewModelScope.launch {
            val subjName = subjects.getOrNull(_selectedSubjectIndex.value)?.name ?: "Cours général"
            repository.toggleAttendance(studentId, subjName)
        }
    }

    fun notifyParentsOfAbsences() {
        viewModelScope.launch {
            repository.notifyParentsOfAbsences()
            showBanner(
                title = "Alertes envoyées aux parents",
                message = "Notifications automatiques transmises aux familles des élèves absents ou en retard.",
                type = "absence"
            )
        }
    }

    fun addDisciplineWarning(studentId: Int, reason: String) {
        viewModelScope.launch {
            val student = students.value.find { it.id == studentId }
            val name = student?.name ?: "Élève"
            repository.addDisciplineWarning(studentId, name, reason)
            showBanner(
                title = "Avertissement enregistré",
                message = "Avertissement ajouté pour $name : $reason",
                type = "system"
            )
        }
    }

    fun setStudentAccessSuspension(studentId: Int, isSuspended: Boolean) {
        viewModelScope.launch {
            repository.setStudentAccessSuspension(studentId, isSuspended)
            val st = students.value.find { it.id == studentId }
            showBanner(
                title = if (isSuspended) "Accès restreint" else "Accès rétabli",
                message = if (isSuspended) "Cours en ligne et PDF bloqués pour ${st?.name}." else "Accès réactivé avec succès pour ${st?.name}.",
                type = if (isSuspended) "alert" else "success"
            )
        }
    }

    fun setDirectorWaiver(studentId: Int, waiver: Boolean) {
        viewModelScope.launch {
            repository.setDirectorWaiver(studentId, waiver)
            val st = students.value.find { it.id == studentId }
            showBanner(
                title = "Dérogation Directeur",
                message = "Dérogation ${if (waiver) "accordée" else "retirée"} pour ${st?.name}.",
                type = "director"
            )
        }
    }

    fun sendTuitionReminder(studentId: Int) {
        viewModelScope.launch {
            repository.sendTuitionReminder(studentId)
            val st = students.value.find { it.id == studentId }
            showBanner(
                title = "Relance envoyée",
                message = "Notification et lien de paiement Mobile Money transmis au parent de ${st?.name}.",
                type = "finance"
            )
        }
    }

    fun addTextbookEntry(
        className: String,
        subjectName: String,
        teacherName: String,
        chapterTitle: String,
        summary: String,
        homework: String,
        progress: Int
    ) {
        viewModelScope.launch {
            repository.addTextbookEntry(className, subjectName, teacherName, chapterTitle, summary, homework, progress)
            showBanner(
                title = "Chapitre enregistré",
                message = "Le cahier de texte a été mis à jour pour la classe de $className.",
                type = "textbook"
            )
        }
    }

    fun validateTextbookEntry(id: Long, comment: String) {
        viewModelScope.launch {
            repository.validateTextbookEntry(id, comment)
            showBanner(
                title = "Cahier de texte validé",
                message = "Le visa de la Direction a été apposé avec succès.",
                type = "textbook"
            )
        }
    }

    fun recordGateCheckIn(studentId: Int, gateType: String = "ENTREE") {
        viewModelScope.launch {
            val record = repository.recordGateCheckIn(studentId, gateType)
            if (record != null) {
                showBanner(
                    title = "Pointage portail ($gateType)",
                    message = "${record.studentName} pointé(e). Alerte WhatsApp transmise au parent (${record.parentWhatsApp}).",
                    type = "gate"
                )
            }
        }
    }

    fun createSchoolTenant(name: String, city: String, code: String, phone: String, waveId: String, orangeId: String) {
        viewModelScope.launch {
            repository.createSchoolTenant(name, city, code, phone, waveId, orangeId)
            showBanner(
                title = "Établissement créé",
                message = "Nouvelle école « $name » configurée avec isolation complète des données.",
                type = "tenant"
            )
        }
    }

    fun duplicateSchoolTenant(sourceTenantId: String, newName: String, newCode: String, newCity: String) {
        viewModelScope.launch {
            repository.duplicateSchoolTenant(sourceTenantId, newName, newCode, newCity)
            showBanner(
                title = "Établissement dupliqué",
                message = "L'école « $newName » a été dupliquée en 1 clic avec son paramétrage.",
                type = "tenant"
            )
        }
    }

    fun processPayment(studentId: Int, amount: Long, method: String) {
        viewModelScope.launch {
            val receiptNum = repository.processTuitionPayment(studentId, amount, method)
            if (receiptNum.isNotEmpty()) {
                val student = students.value.find { it.id == studentId }
                showBanner(
                    title = "Paiement encaissé ($receiptNum)",
                    message = "${EduGestRepository.formatAmount(amount)} FCFA reçus pour ${student?.name}. Reçu généré avec succès.",
                    type = "finance"
                )
            }
        }
    }

    fun publishBulletins(trimester: Int) {
        viewModelScope.launch {
            repository.publishBulletins(trimester)
            showBanner(
                title = "Bulletins publiés",
                message = "Les bulletins du Trimestre $trimester sont à présent accessibles aux parents.",
                type = "bulletin"
            )
        }
    }

    fun broadcastDirectionMessage(target: String, messageText: String) {
        viewModelScope.launch {
            repository.broadcastDirectionMessage(target, messageText)
            showBanner(
                title = "Message diffusé",
                message = "Message envoyé avec succès à : $target",
                type = "message"
            )
        }
    }

    // Instant Messaging Send & Auto-Reply Flow
    fun sendDirectMessage(content: String) {
        val convId = _activeConversationId.value ?: return
        val currentConv = conversations.value.find { it.id == convId } ?: return
        val role = _currentRole.value

        val senderName = when (role) {
            UserRole.DIRECTION -> "Direction (M. le Principal)"
            UserRole.CAISSE -> "Service Comptabilité"
            UserRole.ENSEIGNANT -> subjects[_selectedSubjectIndex.value].teacherName
            UserRole.EDUCATEUR -> "Vie Scolaire (Éducateur)"
            UserRole.PARENT -> "Famille ${students.value.getOrNull(_selectedStudentId.value)?.name?.split(" ")?.last() ?: "Diallo"}"
            UserRole.ELEVE -> students.value.getOrNull(_selectedStudentId.value)?.name ?: "Élève 3e A"
        }

        viewModelScope.launch {
            val sentMsg = repository.sendChatMessage(
                conversationId = convId,
                senderId = role.name.lowercase(),
                senderName = senderName,
                senderRole = role,
                recipientId = currentConv.contactId,
                recipientName = currentConv.contactName,
                content = content
            )

            // Simulate network delivery progression
            delay(500)
            repository.updateMessageStatus(sentMsg.id, MessageStatus.DELIVERED)

            // Simulate realistic reply & read receipt from the other contact
            simulateContactResponse(currentConv, content, sentMsg.id)
        }
    }

    private suspend fun simulateContactResponse(
        conv: Conversation,
        userContent: String,
        userMessageId: String
    ) {
        delay(1200)
        // Mark user's message as READ (double blue check)
        repository.updateMessageStatus(userMessageId, MessageStatus.READ)

        // Show typing indicator
        _isContactTyping.value = true
        delay(2000)
        _isContactTyping.value = false

        // Realistic contextual reply
        val replyContent = generateContextualReply(conv.contactRole, conv.contactName, userContent)

        repository.sendChatMessage(
            conversationId = conv.id,
            senderId = conv.contactId,
            senderName = conv.contactName,
            senderRole = conv.contactRole,
            recipientId = _currentRole.value.name.lowercase(),
            recipientName = _currentRole.value.displayName,
            content = replyContent
        )

        // Trigger in-app notification if still in app
        showBanner(
            title = "Nouveau message de ${conv.contactName}",
            message = replyContent,
            type = "chat"
        )
    }

    private fun generateContextualReply(
        contactRole: UserRole,
        contactName: String,
        userContent: String
    ): String {
        val lower = userContent.lowercase()
        return when (contactRole) {
            UserRole.ENSEIGNANT -> when {
                lower.contains("note") || lower.contains("devoir") || lower.contains("moyenne") ->
                    "Bien reçu. Les copies ont été corrigées avec attention et les observations détaillées figurent sur le bulletin."
                lower.contains("absent") || lower.contains("retard") || lower.contains("justifi") ->
                    "Merci de m'avoir prévenu(e). J'ai annoté le registre d'appel et transmis les documents à la vie scolaire."
                else ->
                    "Merci pour votre message. Je reste à votre disposition lors de ma prochaine permanence du mardi après-midi."
            }
            UserRole.DIRECTION -> when {
                lower.contains("bulletin") || lower.contains("conseil") ->
                    "La Direction confirme que les décisions du conseil de classe ont été validées."
                lower.contains("rendez-vous") || lower.contains("rencontre") ->
                    "Le secrétariat vous contactera dans les plus brefs délais pour convenir d'un créneau."
                else ->
                    "Le message a bien été pris en compte par la Direction du GROUPE SCOLAIRE LASME."
            }
            UserRole.CAISSE -> when {
                lower.contains("paiement") || lower.contains("reçu") || lower.contains("tranche") ->
                    "Votre versement est validé en caisse. Le reçu officiel est téléchargeable dans votre espace."
                else ->
                    "Le service comptabilité est ouvert du lundi au vendredi de 8h à 15h30 pour toute opération financière."
            }
            UserRole.EDUCATEUR -> when {
                lower.contains("absence") || lower.contains("retard") || lower.contains("justifi") ->
                    "La vie scolaire a bien enregistré le justificatif. L'assiduité de l'élève a été mise à jour."
                lower.contains("discipline") || lower.contains("sanction") ->
                    "Le dossier d'incident a été transmis au bureau du Proviseur adjoint."
                else ->
                    "La vie scolaire reste joignable au bureau 102 durant toute la journée de cours."
            }
            UserRole.PARENT -> when {
                lower.contains("bulletin") || lower.contains("félicitation") ->
                    "Nous vous remercions infiniment pour votre suivi et votre engagement auprès de notre enfant."
                lower.contains("absence") || lower.contains("retard") ->
                    "Veuillez nous excuser, un justificatif médical sera déposé dès demain matin à l'accueil."
                else ->
                    "Merci pour l'information, nous prenons bonne note de votre retour."
            }
            UserRole.ELEVE -> when {
                lower.contains("devoir") || lower.contains("cours") || lower.contains("note") ->
                    "Merci pour les explications, j'ai bien relu la leçon et refait les exercices."
                else ->
                    "Bien reçu, merci."
            }
        }
    }

    fun startNewConversation(contact: ChatContact, topic: String) {
        viewModelScope.launch {
            val convId = repository.createOrGetConversation(
                contactName = contact.name,
                contactRole = contact.role,
                contactId = contact.id,
                topic = topic
            )
            _activeConversationId.value = convId
            // Jump to messaging tab
            val tabIdx = _currentRole.value.tabs.indexOf("Messagerie").takeIf { it >= 0 }
                ?: _currentRole.value.tabs.indexOf("Messages").coerceAtLeast(0)
            _currentTab.value = tabIdx
        }
    }

    private fun showBanner(title: String, message: String, type: String) {
        _inAppNotification.value = NotificationEntry(
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis()
        )
    }

    val availableContacts: List<ChatContact>
        get() {
            val list = mutableListOf<ChatContact>()
            // Administration
            list.add(ChatContact("direction", "Direction (M. le Principal)", UserRole.DIRECTION, "Administration", "Direction générale GROUPE SCOLAIRE LASME"))
            list.add(ChatContact("caisse_main", "Service Caisse & Comptabilité", UserRole.CAISSE, "Finances", "Frais de scolarité & reçus"))

            // Teachers
            subjects.forEach { subj ->
                list.add(
                    ChatContact(
                        id = "prof_${subj.id}",
                        name = "${subj.teacherName} (${subj.name})",
                        role = UserRole.ENSEIGNANT,
                        roleTitle = "Professeur de ${subj.name}",
                        detail = "Enseignant classe de 3e A"
                    )
                )
            }

            // Parents
            students.value.forEach { st ->
                list.add(
                    ChatContact(
                        id = "parent_${st.id}",
                        name = "Famille ${st.name.split(" ").last()} (Parent de ${st.name})",
                        role = UserRole.PARENT,
                        roleTitle = "Parent d'élève",
                        detail = "Classe de 3e A"
                    )
                )
            }
            return list
        }
}
