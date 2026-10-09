package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChatContact
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.ConversationsListScreen
import com.example.ui.screens.chat.NewMessageDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduGestViewModel

@Composable
fun MainAppScreen(
    viewModel: EduGestViewModel = viewModel()
) {
    val context = LocalContext.current
    val authenticatedUser by viewModel.authenticatedUser.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()

    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedStudentId by viewModel.selectedStudentId.collectAsStateWithLifecycle()
    val selectedTrimester by viewModel.selectedTrimester.collectAsStateWithLifecycle()
    val selectedSubjectIndex by viewModel.selectedSubjectIndex.collectAsStateWithLifecycle()

    val students by viewModel.students.collectAsStateWithLifecycle()
    val usersList by viewModel.users.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val isContactTyping by viewModel.isContactTyping.collectAsStateWithLifecycle()
    val inAppNotification by viewModel.inAppNotification.collectAsStateWithLifecycle()

    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val selectedClassForTimetable by viewModel.selectedClassForTimetable.collectAsStateWithLifecycle()
    val homeworkExercises by viewModel.homeworkExercises.collectAsStateWithLifecycle()
    val onlineCourses by viewModel.onlineCourses.collectAsStateWithLifecycle()
    val textbookEntries by viewModel.textbookEntries.collectAsStateWithLifecycle()
    val gateCheckIns by viewModel.gateCheckIns.collectAsStateWithLifecycle()
    val schoolTenants by viewModel.schoolTenants.collectAsStateWithLifecycle()

    var showNewMessageDialog by remember { mutableStateOf(false) }

    val auth = authenticatedUser
    // If not authenticated, require login first!
    if (auth == null) {
        LoginScreen(
            errorMessage = loginError,
            isLoading = isAuthenticating,
            onLogin = { u, p -> viewModel.login(u, p) },
            onQuickLogin = { role -> viewModel.quickLogin(role) },
            onGoogleSignIn = { name, email -> viewModel.onGoogleSignIn(name, email) }
        )
        return
    }

    val unreadNotifsCount = notifications.count { !it.isRead }
    val unreadMessagesCount = conversations.sumOf { it.unreadCount }

    val activeConversation = conversations.find { it.id == activeConvId }

    // If a conversation is active, show the 1-on-1 direct chat view
    if (activeConversation != null) {
        ChatDetailScreen(
            conversation = activeConversation,
            messages = activeMessages,
            currentRole = currentRole,
            isTyping = isContactTyping,
            onSendMessage = { text -> viewModel.sendDirectMessage(text) },
            onBackClick = { viewModel.closeConversation() }
        )
    } else {
        Scaffold(
            topBar = {
                Column {
                    EduGestTopBar(
                        currentRole = currentRole,
                        unreadNotifsCount = unreadNotifsCount,
                        authenticatedUser = auth,
                        onRoleSelected = { role -> viewModel.setRole(role) },
                        onNotificationsClick = {
                            val notifTabIndex = currentRole.tabs.indexOf("Notifications")
                            if (notifTabIndex >= 0) {
                                viewModel.setTab(notifTabIndex)
                            }
                        },
                        onLogoutClick = { viewModel.logout() }
                    )

                    // Role switcher chips (Admin can switch to any view)
                    if (auth.role == UserRole.DIRECTION) {
                        RoleSelectorChips(
                            currentRole = currentRole,
                            onRoleSelected = { role -> viewModel.setRole(role) }
                        )
                    }

                    // Tabs row for current role
                    TabNavigationBar(
                        tabs = currentRole.tabs,
                        selectedTabIndex = currentTab.coerceIn(0, currentRole.tabs.size - 1),
                        onTabSelected = { idx -> viewModel.setTab(idx) },
                        unreadMessagesCount = unreadMessagesCount
                    )
                }
            }
        ) { innerPadding ->
            val associatedStudent = if (auth.associatedStudentId != null) {
                students.find { it.id == auth.associatedStudentId }
            } else {
                students.find { it.name.equals(auth.fullName, ignoreCase = true) } ?: students.firstOrNull()
            }
            val canAccessContent = !(associatedStudent != null && (currentRole == UserRole.ELEVE || currentRole == UserRole.PARENT) && associatedStudent.isAccessSuspended && !associatedStudent.directorWaiver)
            val activeLiveCourse = onlineCourses.firstOrNull { it.isActive }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Priority Banner for ELEVE and PARENT when live course is active
                if (activeLiveCourse != null && (currentRole == UserRole.ELEVE || currentRole == UserRole.PARENT)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF075E54)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(shape = CircleShape, color = EduRedAlert, modifier = Modifier.size(10.dp)) {}
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("COURS EN DIRECT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF25D366)) {
                                            Text(
                                                text = activeLiveCourse.className,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${activeLiveCourse.subjectName} : ${activeLiveCourse.topic}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (!canAccessContent) {
                                        Toast.makeText(context, "Accès restreint par la comptabilité pour impayé", Toast.LENGTH_LONG).show()
                                        val payTab = currentRole.tabs.indexOf("Paiements").takeIf { it >= 0 } ?: currentRole.tabs.indexOf("Finances")
                                        if (payTab >= 0) viewModel.setTab(payTab)
                                    } else {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeLiveCourse.meetingUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val idx = currentRole.tabs.indexOf("Cours en ligne")
                                            if (idx >= 0) viewModel.setTab(idx)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejoindre 1-clic", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val currentTabTitle = currentRole.tabs.getOrElse(currentTab) { currentRole.tabs.first() }

                    when (currentTabTitle) {
                        "Administration" -> {
                            val overallAvg = viewModel.getOverallClassAverage(selectedTrimester)
                            val reportCard = viewModel.getStudentReportCard(selectedStudentId, selectedTrimester)

                            AdministrationScreen(
                                students = students,
                                selectedStudentId = selectedStudentId,
                                selectedTrimester = selectedTrimester,
                                overallClassAverage = overallAvg,
                                reportCard = reportCard,
                                subjects = viewModel.subjects,
                                onStudentSelected = { viewModel.setSelectedStudentId(it) },
                                onTrimesterSelected = { viewModel.setSelectedTrimester(it) },
                                onPublishBulletins = { t -> viewModel.publishBulletins(t) },
                                onContactTeacher = { subj ->
                                    val contact = ChatContact(
                                        id = "prof_${subj.id}",
                                        name = "${subj.teacherName} (${subj.name})",
                                        role = UserRole.ENSEIGNANT,
                                        roleTitle = "Professeur de ${subj.name}",
                                        detail = "Classe de 3e A"
                                    )
                                    viewModel.startNewConversation(contact, "Coordination pédagogique")
                                },
                                onBroadcastMessage = { target, msg ->
                                    viewModel.broadcastDirectionMessage(target, msg)
                                },
                                onUpdateStudentWhatsApp = { stId, whatsapp, cls ->
                                    viewModel.updateStudentWhatsApp(stId, whatsapp, cls)
                                },
                                onRegisterStudent = { name, cls, parent, whatsapp ->
                                    viewModel.registerStudent(name, cls, parent, whatsapp)
                                },
                                textbookEntries = textbookEntries,
                                gateCheckIns = gateCheckIns,
                                schoolTenants = schoolTenants,
                                onValidateTextbook = { id, comm -> viewModel.validateTextbookEntry(id, comm) },
                                onRecordGateCheckIn = { stId, type -> viewModel.recordGateCheckIn(stId, type) },
                                onCreateSchoolTenant = { name, city, code, phone, wave, orange ->
                                    viewModel.createSchoolTenant(name, city, code, phone, wave, orange)
                                },
                                onDuplicateSchoolTenant = { origId, name, code, city ->
                                    viewModel.duplicateSchoolTenant(origId, name, code, city)
                                }
                            )
                        }

                        "Comptes & Accès" -> {
                            UserManagementScreen(
                                users = usersList,
                                students = students,
                                subjects = viewModel.subjects,
                                onCreateAccount = { name, user, pass, role, email, phone, stId, subjId, stClass, ctrlClasses, taughtSubjs, attachedSts ->
                                    viewModel.createUserAccount(name, user, pass, role, email, phone, stId, subjId, stClass, ctrlClasses, taughtSubjs, attachedSts)
                                },
                                onDeleteAccount = { userId ->
                                    viewModel.deleteUserAccount(userId)
                                }
                            )
                        }

                        "Cours en ligne" -> {
                            OnlineCourseScreen(
                                currentRole = currentRole,
                                authenticatedUser = authenticatedUser,
                                students = students,
                                onlineCourses = onlineCourses,
                                subjects = viewModel.subjects,
                                onGoToPayment = {
                                    val idx = currentRole.tabs.indexOf("Finances").takeIf { it >= 0 } ?: currentRole.tabs.indexOf("Paiements")
                                    if (idx >= 0) viewModel.setTab(idx)
                                },
                                onStartCourse = { cls, subj, teacher, topic, url, onLaunched ->
                                    viewModel.startOnlineCourseSession(cls, subj, teacher, topic, url, onLaunched)
                                },
                                onCloseCourse = { courseId ->
                                    viewModel.closeOnlineCourse(courseId)
                                }
                            )
                        }

                        "Finances" -> {
                            FinancesScreen(
                                currentRole = currentRole,
                                students = students,
                                receipts = receipts,
                                onProcessPayment = { stId, amt, method ->
                                    viewModel.processPayment(stId, amt, method)
                                },
                                onContactParent = { st ->
                                    val contact = ChatContact(
                                        id = "parent_${st.id}",
                                        name = "Famille ${st.name.split(" ").last()}",
                                        role = UserRole.PARENT,
                                        roleTitle = "Parent d'élève",
                                        detail = "Suivi scolarité ${st.name}"
                                    )
                                    viewModel.startNewConversation(contact, "Frais de scolarité")
                                },
                                onToggleAccessSuspension = { stId, suspended ->
                                    viewModel.setStudentAccessSuspension(stId, suspended)
                                },
                                onToggleDirectorWaiver = { stId, waiver ->
                                    viewModel.setDirectorWaiver(stId, waiver)
                                },
                                onSendTuitionReminder = { stId ->
                                    viewModel.sendTuitionReminder(stId)
                                }
                            )
                        }

                        "Notes" -> {
                            val classAvg = viewModel.getSubjectClassAverage(selectedSubjectIndex, selectedTrimester)
                            NotesScreen(
                                students = students,
                                subjects = viewModel.subjects,
                                selectedSubjectIndex = selectedSubjectIndex,
                                selectedTrimester = selectedTrimester,
                                classAverage = classAvg,
                                getGrade = { stId, subjId, t -> viewModel.getGrade(stId, subjId, t) },
                                onGradeChanged = { stId, subjId, t, score ->
                                    viewModel.updateGrade(stId, subjId, t, score)
                                },
                                onSubjectSelected = { viewModel.setSelectedSubjectIndex(it) },
                                onTrimesterSelected = { viewModel.setSelectedTrimester(it) }
                            )
                        }

                        "Devoirs & PDF" -> {
                            HomeworkExercisesScreen(
                                currentRole = currentRole,
                                exercises = homeworkExercises,
                                subjects = viewModel.subjects,
                                defaultClass = if (auth.role == UserRole.ELEVE) auth.studentClass.ifBlank { "3e A" } else "3e A",
                                teacherName = auth.fullName.ifBlank { "Professeur" },
                                canAccessContent = canAccessContent,
                                textbookEntries = textbookEntries,
                                onAddTextbookEntry = { cls, subj, tName, chTitle, summary, hw, progress ->
                                    viewModel.addTextbookEntry(cls, subj, tName, chTitle, summary, hw, progress)
                                },
                                onGoToPayment = {
                                    val idx = currentRole.tabs.indexOf("Finances").takeIf { it >= 0 } ?: currentRole.tabs.indexOf("Paiements")
                                    if (idx >= 0) viewModel.setTab(idx)
                                },
                                onSaveExercise = { title, subj, cls, teacher, due, desc, pdfName, pdfSize ->
                                    viewModel.saveHomeworkExercise(title, subj, cls, teacher, due, desc, pdfName, pdfSize)
                                },
                                onDeleteExercise = { exId ->
                                    viewModel.deleteHomeworkExercise(exId)
                                }
                            )
                        }

                    "Présence" -> {
                        PresenceScreen(
                            attendanceList = attendance,
                            students = students,
                            onToggleAttendance = { stId -> viewModel.toggleAttendance(stId) },
                            onNotifyParents = { viewModel.notifyParentsOfAbsences() },
                            onContactParent = { st ->
                                val contact = ChatContact(
                                    id = "parent_${st.id}",
                                    name = "Famille ${st.name.split(" ").last()}",
                                    role = UserRole.PARENT,
                                    roleTitle = "Parent d'élève",
                                    detail = "Assiduité & présence ${st.name}"
                                )
                                viewModel.startNewConversation(contact, "Suivi de présence")
                            }
                        )
                    }

                    "Messagerie", "Messages" -> {
                        ConversationsListScreen(
                            conversations = conversations,
                            currentRole = currentRole,
                            onConversationClick = { convId -> viewModel.openConversation(convId) },
                            onNewMessageClick = { showNewMessageDialog = true }
                        )
                    }

                    "Bulletin", "Mon Bulletin" -> {
                        val reportCard = viewModel.getStudentReportCard(selectedStudentId, selectedTrimester)
                        ParentViewScreen(
                            currentSubTab = "Bulletin",
                            students = students,
                            selectedStudentId = selectedStudentId,
                            selectedTrimester = selectedTrimester,
                            reportCard = reportCard,
                            receipts = receipts,
                            attendanceList = attendance,
                            onStudentSelected = { viewModel.setSelectedStudentId(it) },
                            onTrimesterSelected = { viewModel.setSelectedTrimester(it) },
                            onContactTeacher = {
                                val subj = viewModel.subjects.first()
                                val contact = ChatContact(
                                    id = "prof_${subj.id}",
                                    name = "${subj.teacherName} (Prof. Principal)",
                                    role = UserRole.ENSEIGNANT,
                                    roleTitle = "Professeur Principal",
                                    detail = "Classe de 3e A"
                                )
                                viewModel.startNewConversation(contact, "Questions sur le bulletin")
                            },
                            onProcessInstantPayment = { stId, amt, prov, phone ->
                                viewModel.processInstantPayment(stId, amt, prov, phone)
                            }
                        )
                    }

                    "Paiements" -> {
                        ParentViewScreen(
                            currentSubTab = "Paiements",
                            students = students,
                            selectedStudentId = selectedStudentId,
                            selectedTrimester = selectedTrimester,
                            reportCard = null,
                            receipts = receipts,
                            attendanceList = attendance,
                            onStudentSelected = { viewModel.setSelectedStudentId(it) },
                            onTrimesterSelected = { viewModel.setSelectedTrimester(it) },
                            onContactTeacher = {},
                            onProcessInstantPayment = { stId, amt, prov, phone ->
                                viewModel.processInstantPayment(stId, amt, prov, phone)
                            }
                        )
                    }

                    "Absences", "Assiduité" -> {
                        ParentViewScreen(
                            currentSubTab = "Absences",
                            students = students,
                            selectedStudentId = selectedStudentId,
                            selectedTrimester = selectedTrimester,
                            reportCard = null,
                            receipts = receipts,
                            attendanceList = attendance,
                            onStudentSelected = { viewModel.setSelectedStudentId(it) },
                            onTrimesterSelected = { viewModel.setSelectedTrimester(it) },
                            onContactTeacher = {
                                val contact = ChatContact(
                                    id = "educ.horizon",
                                    name = "Vie Scolaire / Éducateur",
                                    role = UserRole.EDUCATEUR,
                                    roleTitle = "Vie Scolaire",
                                    detail = "Justification d'absence"
                                )
                                viewModel.startNewConversation(contact, "Justification d'absence")
                            },
                            onProcessInstantPayment = { stId, amt, prov, phone ->
                                viewModel.processInstantPayment(stId, amt, prov, phone)
                            }
                        )
                    }

                    "Emploi du temps" -> {
                        TimetableScreen(
                            currentRole = currentRole,
                            slots = timetableSlots,
                            selectedClass = selectedClassForTimetable,
                            onClassSelected = { viewModel.setSelectedClassForTimetable(it) },
                            onSaveSlot = { viewModel.saveTimetableSlot(it) },
                            onDeleteSlot = { viewModel.deleteTimetableSlot(it) },
                            subjects = viewModel.subjects,
                            selectedSubjectIndex = selectedSubjectIndex
                        )
                    }

                    "Notifications" -> {
                        NotificationsScreen(
                            notifications = notifications,
                            currentRole = currentRole,
                            selectedStudentId = selectedStudentId
                        )
                    }
                }

                // In-App Notification Toast Banner (top overlay)
                InAppNotificationBanner(
                    notification = inAppNotification,
                    onDismiss = { viewModel.dismissInAppNotification() },
                    onClick = {
                        val notif = inAppNotification
                        viewModel.dismissInAppNotification()
                        if (notif?.type == "chat") {
                            val msgTab = currentRole.tabs.indexOf("Messagerie").takeIf { it >= 0 }
                                ?: currentRole.tabs.indexOf("Messages").coerceAtLeast(0)
                            viewModel.setTab(msgTab)
                        }
                    }
                )
            }
        }
    }
    }

    // New Message Dialog
    if (showNewMessageDialog) {
        NewMessageDialog(
            availableContacts = viewModel.availableContacts,
            onContactSelected = { contact, topic ->
                viewModel.startNewConversation(contact, topic)
            },
            onDismiss = { showNewMessageDialog = false }
        )
    }
}
