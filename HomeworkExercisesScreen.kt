package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.HomeworkExercise
import com.example.data.model.Subject
import com.example.data.model.TextbookEntry
import com.example.data.model.UserRole
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkExercisesScreen(
    currentRole: UserRole,
    exercises: List<HomeworkExercise>,
    subjects: List<Subject>,
    defaultClass: String = "3e A",
    teacherName: String = "M. Konaté",
    canAccessContent: Boolean = true,
    textbookEntries: List<TextbookEntry> = emptyList(),
    onAddTextbookEntry: ((String, String, String, String, String, String, Int) -> Unit)? = null,
    onGoToPayment: (() -> Unit)? = null,
    onSaveExercise: (String, String, String, String, String, String, String, String) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allClasses = listOf("Toutes", "6e 1", "6e A", "5e A", "4e A", "3e A", "2nde C", "1ère D", "Tle D")
    var selectedClassFilter by remember { mutableStateOf(defaultClass) }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateTextbookDialog by remember { mutableStateOf(false) }
    var viewingExercisePdf by remember { mutableStateOf<HomeworkExercise?>(null) }

    val canPublish = currentRole == UserRole.ENSEIGNANT || currentRole == UserRole.DIRECTION

    val filteredExercises = exercises.filter { ex ->
        val matchesClass = selectedClassFilter == "Toutes" || ex.className.equals(selectedClassFilter, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == null || ex.subjectName.equals(selectedSubjectFilter, ignoreCase = true)
        matchesClass && matchesSubject
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Restriction banner if suspended for unpaid tuition
        if (!canAccessContent && (currentRole == UserRole.ELEVE || currentRole == UserRole.PARENT)) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EduRedLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EduRedAlert)
                            Text("Accès aux devoirs PDF suspendu pour impayé", fontWeight = FontWeight.Bold, color = EduRedAlert, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Le téléchargement des devoirs est temporairement restreint par la Comptabilité. Réglez votre scolarité par Mobile Money (Wave, Orange, MTN, Moov) pour un déblocage automatique instantané, ou sollicitez une dérogation auprès de la Direction.",
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
                                Text("Régler la scolarité par Mobile Money", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Header card
        item {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Devoirs & Exercices en PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduRedLight
                                ) {
                                    Text(
                                        text = "PDF",
                                        fontWeight = FontWeight.Bold,
                                        color = EduRedAlert,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (canPublish)
                                    "Déposez les devoirs, fiches de TD et énoncés avec documents PDF joints"
                                else
                                    "Consultez et téléchargez les devoirs déposés par vos enseignants",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (canPublish) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showCreateTextbookDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = EduIndigo)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cahier de texte", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("publish_homework_btn")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Déposer PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Class selector chips
                    Text(
                        text = "Filtrer par classe :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allClasses.forEach { cls ->
                            val isSel = cls == selectedClassFilter
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedClassFilter = cls },
                                label = { Text(cls, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Subject filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSubjectFilter == null,
                    onClick = { selectedSubjectFilter = null },
                    label = { Text("Toutes matières (${exercises.size})", fontSize = 11.sp) },
                    shape = RoundedCornerShape(99.dp)
                )
                subjects.forEach { subj ->
                    val isSel = selectedSubjectFilter == subj.name
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedSubjectFilter = if (isSel) null else subj.name },
                        label = { Text(subj.name, fontSize = 11.sp) },
                        shape = RoundedCornerShape(99.dp)
                    )
                }
            }
        }

        // Cahier de texte (Suivi des chapitres) overview
        if (textbookEntries.isNotEmpty()) {
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = EduIndigo, modifier = Modifier.size(18.dp))
                                Text("Cahier de texte • Chapitres récents", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (canPublish) {
                                TextButton(onClick = { showCreateTextbookDialog = true }) {
                                    Text("+ Nouveau chapitre", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val recentEntries = textbookEntries.take(3)
                        recentEntries.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(entry.chapterTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (entry.isDirectorValidated) EduGreenLight else EduAmberLight
                                        ) {
                                            Text(
                                                text = if (entry.isDirectorValidated) "Visa Direction ✓" else "En cours",
                                                color = if (entry.isDirectorValidated) EduGreenSuccess else EduAmberWarn,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("${entry.subjectName} • ${entry.className} • ${entry.sessionDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Progression : ${entry.progressPercentage}%", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        LinearProgressIndicator(
                                            progress = { entry.progressPercentage / 100f },
                                            modifier = Modifier.weight(1f).height(4.dp),
                                            color = if (entry.progressPercentage >= 70) EduGreenSuccess else EduIndigo
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty state or List of exercises
        if (filteredExercises.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun devoir disponible pour $selectedClassFilter",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Les exercices déposés par les enseignants apparaîtront ici.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredExercises, key = { it.id }) { ex ->
                HomeworkExerciseCard(
                    exercise = ex,
                    canDelete = canPublish,
                    onViewPdf = { viewingExercisePdf = ex },
                    onDelete = { onDeleteExercise(ex.id) }
                )
            }
        }
    }

    // Modal to create/publish new PDF exercise
    if (showCreateDialog) {
        CreateHomeworkDialog(
            subjects = subjects,
            allClasses = listOf("6e 1", "6e A", "5e A", "4e A", "3e A", "2nde C", "1ère D", "Tle D"),
            defaultTeacherName = teacherName,
            onConfirm = { title, subj, cls, teacher, due, desc, pdfName, pdfSize ->
                onSaveExercise(title, subj, cls, teacher, due, desc, pdfName, pdfSize)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Modal to create textbook chapter (Cahier de texte)
    if (showCreateTextbookDialog) {
        CreateTextbookDialog(
            subjects = subjects,
            allClasses = listOf("6e 1", "6e A", "5e A", "4e A", "3e A", "2nde C", "1ère D", "Tle D"),
            defaultTeacherName = teacherName,
            onConfirm = { cls, subj, tName, chTitle, summary, hw, progress ->
                onAddTextbookEntry?.invoke(cls, subj, tName, chTitle, summary, hw, progress)
                showCreateTextbookDialog = false
            },
            onDismiss = { showCreateTextbookDialog = false }
        )
    }

    // Modal to view simulated PDF
    if (viewingExercisePdf != null) {
        PdfViewerDialog(
            exercise = viewingExercisePdf!!,
            onDismiss = { viewingExercisePdf = null }
        )
    }
}

@Composable
fun HomeworkExerciseCard(
    exercise: HomeworkExercise,
    canDelete: Boolean,
    onViewPdf: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_${exercise.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Subject badge, Class badge, Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduIndigoSurface
                    ) {
                        Text(
                            text = exercise.subjectName,
                            fontWeight = FontWeight.Bold,
                            color = EduIndigo,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = exercise.className,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EduAmberLight
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = EduAmberWarn, modifier = Modifier.size(12.dp))
                        Text(
                            text = "À rendre : ${exercise.dueDate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduAmberWarn
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Teacher info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Text(
                    text = "Enseignant : ${exercise.teacherName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description
            Text(
                text = exercise.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PDF Attachment Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewPdf)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EduRedAlert.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Fichier PDF",
                                tint = EduRedAlert,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = exercise.pdfFileName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Document PDF • ${exercise.pdfFileSize}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onViewPdf,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = EduIndigo.copy(alpha = 0.12f), contentColor = EduIndigo),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ouvrir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (canDelete) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = EduRedAlert)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer ce devoir", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHomeworkDialog(
    subjects: List<Subject>,
    allClasses: List<String>,
    defaultTeacherName: String,
    onConfirm: (String, String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Mathématiques") }
    var selectedClass by remember { mutableStateOf("3e A") }
    var teacherName by remember { mutableStateOf(defaultTeacherName) }
    var dueDate by remember { mutableStateOf("20 Octobre 2026") }
    var description by remember { mutableStateOf("") }
    var pdfFileName by remember { mutableStateOf("Exercices_${selectedSubject.take(5)}_${selectedClass}.pdf") }
    var pdfFileSize by remember { mutableStateOf("1.4 Mo") }

    val context = LocalContext.current
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        pdfFileName = it.getString(nameIdx) ?: pdfFileName
                    }
                    if (sizeIdx >= 0) {
                        val bytes = it.getLong(sizeIdx)
                        pdfFileSize = if (bytes > 1024 * 1024) {
                            String.format(Locale.US, "%.1f Mo", bytes / (1024.0 * 1024.0))
                        } else {
                            "${(bytes / 1024).coerceAtLeast(1)} Ko"
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Déposer un devoir en PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                    Text(
                        text = "Les élèves et leurs parents recevront le document",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) {
                                pdfFileName = it.trim().replace(" ", "_") + ".pdf"
                            }
                        },
                        label = { Text("Titre du devoir") },
                        placeholder = { Text("ex: DM 3 : Systèmes d'équations") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Subject choice
                item {
                    Text(text = "Matière :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { s ->
                            FilterChip(
                                selected = s.name == selectedSubject,
                                onClick = { selectedSubject = s.name },
                                label = { Text(s.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Class choice
                item {
                    Text(text = "Classe concernée :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allClasses.forEach { c ->
                            FilterChip(
                                selected = c == selectedClass,
                                onClick = { selectedClass = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Due date
                item {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date limite de remise") },
                        placeholder = { Text("ex: 25 Octobre 2026") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Consignes & Instructions") },
                        placeholder = { Text("Résoudre les exercices 1 à 4. Feuille double.") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                // PDF File picker from internal storage (Telephone / PC)
                item {
                    Text(text = "Document joint (Stockage interne) :", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EduIndigo.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = EduRedAlert)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pdfFileName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Taille : $pdfFileSize • Document prêt pour diffusion", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        documentPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                                    } catch (e: Exception) {
                                        pdfFileSize = "1.5 Mo"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = EduIndigo)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ouvrir le stockage interne (Téléphone / PC)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Actions
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onConfirm(
                                        title.trim(),
                                        selectedSubject,
                                        selectedClass,
                                        teacherName.ifBlank { "Professeur" },
                                        dueDate.ifBlank { "Prochain cours" },
                                        description.ifBlank { "Voir document PDF ci-joint." },
                                        pdfFileName,
                                        pdfFileSize
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Publier le devoir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfViewerDialog(
    exercise: HomeworkExercise,
    onDismiss: () -> Unit
) {
    var isDownloaded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = EduRedAlert)
                        Text(
                            text = "Aperçu du Document PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Simulated PDF Page
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // School Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GROUPE SCOLAIRE LASME",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EduIndigo
                                )
                                Text("Excellence • Discipline • Réussite", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Classe : ${exercise.className}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("Année : 2026-2027", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                        // Title Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = exercise.title.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Discipline : ${exercise.subjectName} • Professeur : ${exercise.teacherName}",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Exercise Instructions
                        Text(
                            text = "CONSIGNES :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = EduIndigo
                        )
                        Text(
                            text = exercise.description,
                            fontSize = 11.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sample questions layout
                        Text(
                            text = "Exercice 1 (8 points)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "1. Déterminer l'expression de la fonction f(x) sachant que f(2) = 5 et f(-1) = -1.\n2. Tracer la droite représentative dans un repère orthonormé (O, I, J).\n3. Résoudre l'équation f(x) = 0.",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Exercice 2 (12 points)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Soit un triangle ABC tel que AB = 6 cm, AC = 8 cm et BC = 10 cm.\n1. Démontrer que le triangle ABC est rectangle en A.\n2. Calculer l'aire du triangle et la longueur de la hauteur issue de A.",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${exercise.pdfFileName} (${exercise.pdfFileSize})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { isDownloaded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) EduGreenSuccess else EduIndigo
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloaded) "Téléchargé" else "Télécharger PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTextbookDialog(
    subjects: List<Subject>,
    allClasses: List<String>,
    defaultTeacherName: String,
    onConfirm: (String, String, String, String, String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var chapterTitle by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Mathématiques") }
    var selectedClass by remember { mutableStateOf("6e 1") }
    var teacherName by remember { mutableStateOf(defaultTeacherName) }
    var summary by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(50) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cahier de Texte",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saisie du chapitre & Avancement du programme",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                // Chapter Title
                item {
                    OutlinedTextField(
                        value = chapterTitle,
                        onValueChange = { chapterTitle = it },
                        label = { Text("Titre du Chapitre / Leçon *") },
                        placeholder = { Text("ex: Chapitre 4 : Équations & Inéquations") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Class selection
                item {
                    Text(text = "Classe concernée :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allClasses.forEach { cls ->
                            val isSel = cls == selectedClass
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedClass = cls },
                                label = { Text(cls, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Subject selection
                item {
                    Text(text = "Discipline :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { s ->
                            val isSel = s.name == selectedSubject
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedSubject = s.name },
                                label = { Text(s.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Objectives & Summary
                item {
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Objectifs & Contenu abordé *") },
                        placeholder = { Text("Propriétés géométriques, résolution d'exercices d'application...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // Homework assigned
                item {
                    OutlinedTextField(
                        value = homework,
                        onValueChange = { homework = it },
                        label = { Text("Devoir ou travail à faire pour la prochaine séance") },
                        placeholder = { Text("Exercices 3 et 5 page 42 du manuel") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Progress Percentage
                item {
                    Text("Avancement du programme de l'année : $progress%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { progress = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = EduIndigo, activeTrackColor = EduIndigo)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { p ->
                            SuggestionChip(
                                onClick = { progress = p },
                                label = { Text("$p%", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Actions
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (chapterTitle.isNotBlank() && summary.isNotBlank()) {
                                    onConfirm(
                                        selectedClass,
                                        selectedSubject,
                                        teacherName.ifBlank { "Professeur" },
                                        chapterTitle.trim(),
                                        summary.trim(),
                                        homework.trim(),
                                        progress
                                    )
                                }
                            },
                            enabled = chapterTitle.isNotBlank() && summary.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
