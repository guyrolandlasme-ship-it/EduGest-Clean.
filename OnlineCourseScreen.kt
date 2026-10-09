package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

private val WhatsAppGreen = Color(0xFF25D366)
private val WhatsAppDarkGreen = Color(0xFF075E54)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineCourseScreen(
    currentRole: UserRole,
    authenticatedUser: UserAccount?,
    students: List<Student>,
    onlineCourses: List<OnlineCourseSession>,
    subjects: List<Subject>,
    onGoToPayment: (() -> Unit)? = null,
    onStartCourse: (String, String, String, String, String, ((OnlineCourseSession) -> Unit)?) -> Unit,
    onCloseCourse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val availableClasses = listOf("6e 1", "3e A", "5e A", "4e A", "2nde C", "1ère D", "Tle D")

    // Default class: 6e 1 is prominent as requested
    var selectedClass by remember {
        mutableStateOf(
            if (authenticatedUser?.role == UserRole.ELEVE) {
                authenticatedUser.studentClass.ifBlank { "6e 1" }
            } else {
                "6e 1"
            }
        )
    }

    val defaultSubject = subjects.firstOrNull { it.id == (authenticatedUser?.subjectId ?: 0) }?.name ?: "Mathématiques"
    var selectedSubjectName by remember { mutableStateOf(defaultSubject) }
    var courseTopic by remember { mutableStateOf("Calcul littéral & Équations du 1er degré") }
    var customMeetingUrl by remember { mutableStateOf("https://meet.google.com/lasme-6e1-maths") }

    // Active course session (if any currently active for selected or user's class)
    val activeSession = onlineCourses.firstOrNull { it.isActive }

    // Broadcast Dialog state
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var lastLaunchedSession by remember { mutableStateOf<OnlineCourseSession?>(null) }

    // Filter students belonging to selected class
    val classStudents = students.filter { it.className.equals(selectedClass, ignoreCase = true) }
    val readyWhatsappCount = classStudents.count { it.whatsappNumber.isNotBlank() }

    // Live elapsed timer for active session
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - activeSession.startedAt) / 1000).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    val elapsedFormatted = String.format(
        "%02d:%02d:%02d",
        elapsedSeconds / 3600,
        (elapsedSeconds % 3600) / 60,
        elapsedSeconds % 60
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Hero Banner: Module Cours en ligne via WhatsApp
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(WhatsAppDarkGreen, Color(0xFF128C7E), EduIndigo)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WhatsAppGreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "GROUPE SCOLAIRE LASME",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Cours en ligne via WhatsApp",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Diffusion instantanée et automatique des liens vidéo sur les numéros WhatsApp de tous les élèves de la classe sélectionnée.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Access Restriction Banner if student has suspended tuition access
        val associatedStudent = if (authenticatedUser?.associatedStudentId != null) {
            students.find { it.id == authenticatedUser.associatedStudentId }
        } else {
            students.find { it.name.equals(authenticatedUser?.fullName, ignoreCase = true) } ?: students.firstOrNull()
        }
        val isStudentRestricted = (currentRole == UserRole.ELEVE || currentRole == UserRole.PARENT) &&
                associatedStudent != null && !associatedStudent.canAccessOnlineContent

        if (isStudentRestricted) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EduRedLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EduRedAlert)
                            Text("Accès aux cours en direct suspendu pour impayé", fontWeight = FontWeight.Bold, color = EduRedAlert, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Le service comptabilité a temporairement suspendu l'accès aux cours vidéo et devoirs PDF pour ${associatedStudent?.name} (Solde restant : ${associatedStudent?.remainingBalance} FCFA). Réglez par Mobile Money (Wave, Orange, MTN, Moov) pour un déblocage automatique instantané, ou sollicitez une dérogation de la Direction.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (onGoToPayment != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onGoToPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Payer par Mobile Money (Déblocage immédiat)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Active Live Session Banner (if ongoing)
        if (activeSession != null) {
            item {
                ActiveLiveCourseCard(
                    session = activeSession,
                    elapsedFormatted = elapsedFormatted,
                    students = students.filter { it.className.equals(activeSession.className, ignoreCase = true) },
                    currentRole = currentRole,
                    onJoinMeeting = {
                        if (isStudentRestricted) {
                            Toast.makeText(context, "Accès restreint par la comptabilité pour impayé", Toast.LENGTH_LONG).show()
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeSession.meetingUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lien copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString(activeSession.meetingUrl))
                            }
                        }
                    },
                    onOpenWhatsAppBroadcast = {
                        openWhatsAppBroadcastIntent(context, activeSession)
                    },
                    onCloseSession = {
                        onCloseCourse(activeSession.id)
                    },
                    onCopyLink = {
                        clipboardManager.setText(AnnotatedString(activeSession.meetingUrl))
                        Toast.makeText(context, "Lien copié : ${activeSession.meetingUrl}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Section for Teacher and Administration: Launch online course
        if (currentRole == UserRole.ENSEIGNANT || currentRole == UserRole.DIRECTION) {
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EduSky.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CastForEducation,
                                            contentDescription = null,
                                            tint = EduSky,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Lancer un nouveau cours en ligne",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Choisissez la classe et diffusez automatiquement le lien vidéo",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Class Selection Chips (prominent 6e 1)
                        Text(
                            text = "1. Choisir la classe concernée *",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableClasses.forEach { cls ->
                                val isSelected = cls == selectedClass
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedClass = cls
                                        customMeetingUrl = "https://meet.google.com/lasme-${cls.lowercase().replace(" ", "")}-maths"
                                    },
                                    label = {
                                        Text(
                                            text = cls,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WhatsAppDarkGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(99.dp),
                                    modifier = Modifier.testTag("class_chip_$cls")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // WhatsApp readiness indicator card for selected class
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (readyWhatsappCount > 0) WhatsAppGreen.copy(alpha = 0.12f) else EduAmberWarn.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (readyWhatsappCount > 0) WhatsAppGreen.copy(alpha = 0.4f) else EduAmberWarn.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (readyWhatsappCount > 0) WhatsAppDarkGreen else EduAmberWarn,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$readyWhatsappCount / ${classStudents.size} élèves prêts sur WhatsApp ($selectedClass)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (readyWhatsappCount > 0) WhatsAppDarkGreen else EduAmberWarn
                                    )
                                    Text(
                                        text = "Numéros enregistrés par l'administration dans le profil élève.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Preview of students in this class
                        if (classStudents.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Élèves de $selectedClass qui recevront le lien :",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                classStudents.forEach { st ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = WhatsAppGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${st.name} (${st.whatsappNumber.ifBlank { "Sans N°" }})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Subject Selection
                        Text(
                            text = "2. Matière dispensée *",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { subj ->
                                val isSel = subj.name == selectedSubjectName
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedSubjectName = subj.name },
                                    label = { Text(subj.name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EduSky,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Topic of course
                        OutlinedTextField(
                            value = courseTopic,
                            onValueChange = { courseTopic = it },
                            label = { Text("Thème / Chapitre du cours *") },
                            placeholder = { Text("ex: Calcul littéral & Théorème de Thalès") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. Video Meeting URL
                        OutlinedTextField(
                            value = customMeetingUrl,
                            onValueChange = { customMeetingUrl = it },
                            label = { Text("Lien du cours vidéo (Google Meet / Jitsi) *") },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = EduIndigo)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val rand = UUID.randomUUID().toString().take(6)
                                    customMeetingUrl = "https://meet.google.com/lasme-${selectedClass.lowercase().replace(" ", "")}-$rand"
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Régénérer lien")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button: « Démarrer cours en ligne »
                        Button(
                            onClick = {
                                val teacher = authenticatedUser?.fullName ?: "M. Konaté (Professeur)"
                                onStartCourse(
                                    selectedClass,
                                    selectedSubjectName,
                                    teacher,
                                    courseTopic.ifBlank { "Cours en direct de $selectedSubjectName" },
                                    customMeetingUrl
                                ) { session ->
                                    lastLaunchedSession = session
                                    showBroadcastDialog = true
                                }
                            },
                            enabled = selectedClass.isNotBlank() && selectedSubjectName.isNotBlank() && customMeetingUrl.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppDarkGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_online_course_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Démarrer cours en ligne & Diffuser WhatsApp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Section for Student and Parent
        if (currentRole == UserRole.ELEVE || currentRole == UserRole.PARENT) {
            item {
                StudentParentOnlineCourseCard(
                    currentRole = currentRole,
                    authenticatedUser = authenticatedUser,
                    activeSession = activeSession,
                    onJoin = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lien copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
                            clipboardManager.setText(AnnotatedString(url))
                        }
                    }
                )
            }
        }

        // History of Online Courses
        item {
            Text(
                text = "Historique des sessions de cours en ligne",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        val pastCourses = onlineCourses.filter { !it.isActive }
        if (pastCourses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aucune session archivée pour l'instant.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(pastCourses) { course ->
                PastCourseCard(course = course)
            }
        }
    }

    // Modal: Confirmation de la Diffusion Automatique sur WhatsApp
    if (showBroadcastDialog && lastLaunchedSession != null) {
        val session = lastLaunchedSession!!
        val recipients = students.filter { it.className.equals(session.className, ignoreCase = true) }

        BroadcastSuccessDialog(
            session = session,
            recipients = recipients,
            onDismiss = { showBroadcastDialog = false },
            onOpenWhatsApp = {
                openWhatsAppBroadcastIntent(context, session)
                showBroadcastDialog = false
            }
        )
    }
}

@Composable
fun ActiveLiveCourseCard(
    session: OnlineCourseSession,
    elapsedFormatted: String,
    students: List<Student>,
    currentRole: UserRole,
    onJoinMeeting: () -> Unit,
    onOpenWhatsAppBroadcast: () -> Unit,
    onCloseSession: () -> Unit,
    onCopyLink: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, WhatsAppGreen, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = EduRedAlert,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(8.dp)) {}
                        Text(
                            text = "EN DIRECT • $elapsedFormatted",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduIndigo.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Classe ${session.className}",
                        color = EduIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${session.subjectName} : ${session.topic}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enseignant : ${session.teacherName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Video stream / preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Flux vidéo sécurisé de la salle de classe",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = session.meetingUrl,
                        color = WhatsAppGreen,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diffusion status banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WhatsAppGreen.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SendToMobile,
                        contentDescription = null,
                        tint = WhatsAppDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Lien diffusé instantanément sur WhatsApp aux ${students.size} élèves de ${session.className}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WhatsAppDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onJoinMeeting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rejoindre Meet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onOpenWhatsAppBroadcast,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Diffuser WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCopyLink) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copier le lien", fontSize = 11.sp)
                }

                if (currentRole == UserRole.ENSEIGNANT || currentRole == UserRole.DIRECTION) {
                    OutlinedButton(
                        onClick = onCloseSession,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EduRedAlert),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clôturer le cours", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentParentOnlineCourseCard(
    currentRole: UserRole,
    authenticatedUser: UserAccount?,
    activeSession: OnlineCourseSession?,
    onJoin: (String) -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Espace ${currentRole.displayName} • Cours en Ligne",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val userClass = authenticatedUser?.studentClass ?: "6e 1"
            Text(
                text = "Classe rattachée : $userClass",
                fontSize = 12.sp,
                color = EduIndigo,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (activeSession != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhatsAppGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(shape = CircleShape, color = EduRedAlert, modifier = Modifier.size(8.dp)) {}
                            Text(
                                text = "COURS EN DIRECT EN COURS",
                                color = EduRedAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${activeSession.subjectName} avec ${activeSession.teacherName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = activeSession.topic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Le lien de connexion a été diffusé automatiquement sur votre numéro WhatsApp.",
                            fontSize = 11.sp,
                            color = WhatsAppDarkGreen
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onJoin(activeSession.meetingUrl) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rejoindre la session vidéo maintenant", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EduIndigo)
                            Text(
                                text = "Aucun cours en direct actuellement",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dès que le professeur lance un cours en ligne pour votre classe, vous recevrez automatiquement le lien vidéo sur votre WhatsApp.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastCourseCard(course: OnlineCourseSession) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(course.startedAt))

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EduIndigo.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = course.className,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = course.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = course.topic,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${course.teacherName} • Diffusé à ${course.whatsappSentCount} élèves sur WhatsApp • $dateFmt",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Terminé",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BroadcastSuccessDialog(
    session: OnlineCourseSession,
    recipients: List<Student>,
    onDismiss: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = WhatsAppGreen,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cours en ligne Démarré !",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Diffusion automatique sur WhatsApp réussie",
                    fontSize = 13.sp,
                    color = WhatsAppDarkGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Classe : ${session.className} • ${session.subjectName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Lien vidéo : ${session.meetingUrl}",
                            fontSize = 11.sp,
                            color = EduIndigo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${recipients.size} destinataires WhatsApp notifiés :",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recipients.take(5).forEach { st ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = st.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = st.whatsappNumber.ifBlank { "+225 ..." },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continuer")
                    }

                    Button(
                        onClick = onOpenWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ouvrir WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun openWhatsAppBroadcastIntent(context: android.content.Context, session: OnlineCourseSession) {
    try {
        val text = "🔴 *GROUPE SCOLAIRE LASME — COURS EN LIGNE EN DIRECT*\n\n" +
                "📚 *Classe :* ${session.className}\n" +
                "📖 *Matière :* ${session.subjectName}\n" +
                "👨‍🏫 *Enseignant :* ${session.teacherName}\n" +
                "🎯 *Thème :* ${session.topic}\n\n" +
                "👉 *Rejoignez immédiatement la session vidéo :*\n${session.meetingUrl}\n\n" +
                "_Message automatique diffusé à tous les élèves de la classe ${session.className}._"

        val encoded = URLEncoder.encode(text, "UTF-8")
        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=$encoded")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Ouverture de WhatsApp impossible sur ce terminal", Toast.LENGTH_SHORT).show()
    }
}
