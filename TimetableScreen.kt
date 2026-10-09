package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Subject
import com.example.data.model.TimetableSlot
import com.example.data.model.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    currentRole: UserRole,
    slots: List<TimetableSlot>,
    selectedClass: String,
    onClassSelected: (String) -> Unit,
    onSaveSlot: (TimetableSlot) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    subjects: List<Subject>,
    selectedSubjectIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val allClasses = listOf("6e A", "5e A", "4e A", "3e A", "2nde C", "1ère D", "Tle D")
    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

    var selectedDayFilter by remember { mutableStateOf<String?>(null) }
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val canManageTimetable = currentRole == UserRole.EDUCATEUR || currentRole == UserRole.DIRECTION

    // Filter slots by selected class
    val classSlots = slots.filter { it.className.equals(selectedClass, ignoreCase = true) }
    val displaySlots = if (selectedDayFilter == null) {
        classSlots
    } else {
        classSlots.filter { it.day.equals(selectedDayFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Header Card & Class Selector
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
                                    text = "Emploi du temps officiel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduIndigoSurface
                                ) {
                                    Text(
                                        text = selectedClass,
                                        fontWeight = FontWeight.Bold,
                                        color = EduIndigo,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (canManageTimetable)
                                    "Gestion & modification des plannings de cours par classe"
                                else
                                    "Consultez les créneaux et salles de cours pour $selectedClass",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (canManageTimetable) {
                            Button(
                                onClick = {
                                    slotToEdit = TimetableSlot(
                                        id = 0,
                                        className = selectedClass,
                                        day = "Lundi",
                                        startTime = "08:00",
                                        endTime = "10:00",
                                        subjectName = "Mathématiques",
                                        teacherName = "M. Konaté",
                                        classroom = "Salle $selectedClass"
                                    )
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_timetable_slot_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Class selector chips (Educateur can switch to ANY class!)
                    Text(
                        text = "Sélectionner la classe :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allClasses) { cls ->
                            val isSel = cls == selectedClass
                            FilterChip(
                                selected = isSel,
                                onClick = { onClassSelected(cls) },
                                label = { Text(cls, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EduIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Days Filter Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedDayFilter == null,
                    onClick = { selectedDayFilter = null },
                    label = { Text("Toute la semaine (${classSlots.size})", fontSize = 12.sp) },
                    shape = RoundedCornerShape(99.dp)
                )
                days.forEach { day ->
                    val count = classSlots.count { it.day.equals(day, ignoreCase = true) }
                    FilterChip(
                        selected = selectedDayFilter == day,
                        onClick = { selectedDayFilter = if (selectedDayFilter == day) null else day },
                        label = { Text("$day ($count)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(99.dp)
                    )
                }
            }
        }

        // List of Course Slots Grouped or by day
        if (displaySlots.isEmpty()) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun cours programmé pour $selectedClass ${selectedDayFilter ?: ""}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (canManageTimetable) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Touchez « Ajouter » ci-dessus pour insérer un créneau horaire.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Group by Day for clarity
            val groupedByDay = displaySlots.groupBy { it.day }
            days.filter { groupedByDay.containsKey(it) }.forEach { dayName ->
                val daySlots = groupedByDay[dayName] ?: emptyList()

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduIndigo.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = dayName.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = EduIndigo,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "${daySlots.size} cours prévus",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(daySlots, key = { it.id }) { slot ->
                    TimetableSlotCard(
                        slot = slot,
                        canManage = canManageTimetable,
                        onEditClick = {
                            slotToEdit = slot
                            showAddDialog = true
                        },
                        onDeleteClick = {
                            onDeleteSlot(slot.id)
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Slot Dialog
    if (showAddDialog && slotToEdit != null) {
        TimetableSlotDialog(
            initialSlot = slotToEdit!!,
            allClasses = allClasses,
            subjects = subjects,
            onConfirm = { savedSlot ->
                onSaveSlot(savedSlot)
                showAddDialog = false
                slotToEdit = null
            },
            onDelete = if (slotToEdit!!.id != 0L) {
                {
                    onDeleteSlot(slotToEdit!!.id)
                    showAddDialog = false
                    slotToEdit = null
                }
            } else null,
            onDismiss = {
                showAddDialog = false
                slotToEdit = null
            }
        )
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    canManage: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val subjectColor = when {
        slot.subjectName.contains("Math", ignoreCase = true) -> EduIndigo
        slot.subjectName.contains("Franç", ignoreCase = true) -> EduSky
        slot.subjectName.contains("Physiq", ignoreCase = true) || slot.subjectName.contains("Chim", ignoreCase = true) -> EduGold
        slot.subjectName.contains("SVT", ignoreCase = true) || slot.subjectName.contains("Bio", ignoreCase = true) -> EduGreenSuccess
        slot.subjectName.contains("Hist", ignoreCase = true) || slot.subjectName.contains("Géo", ignoreCase = true) -> EduAmberWarn
        slot.subjectName.contains("Anglais", ignoreCase = true) -> Color(0xFF673AB7)
        slot.subjectName.contains("EPS", ignoreCase = true) -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canManage, onClick = onEditClick)
            .testTag("slot_item_${slot.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left color accent bar + Time column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(subjectColor)
                )

                Column {
                    Text(
                        text = "${slot.startTime} - ${slot.endTime}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = slot.classroom,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Subject and Teacher details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = slot.subjectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = subjectColor
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = slot.teacherName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action icons if manager
            if (canManage) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier créneau",
                            tint = EduIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer créneau",
                            tint = EduRedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSlotDialog(
    initialSlot: TimetableSlot,
    allClasses: List<String>,
    subjects: List<Subject>,
    onConfirm: (TimetableSlot) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

    var selectedClass by remember { mutableStateOf(initialSlot.className) }
    var selectedDay by remember { mutableStateOf(initialSlot.day) }
    var startTime by remember { mutableStateOf(initialSlot.startTime) }
    var endTime by remember { mutableStateOf(initialSlot.endTime) }
    var subjectName by remember { mutableStateOf(initialSlot.subjectName) }
    var teacherName by remember { mutableStateOf(initialSlot.teacherName) }
    var classroom by remember { mutableStateOf(initialSlot.classroom) }

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
                            text = if (initialSlot.id == 0L) "Ajouter un cours" else "Modifier le cours",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                    Text(
                        text = "Édition de l'emploi du temps pour le Groupe Scolaire Lasme",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Classroom / Class Choice
                item {
                    Text(text = "Salle / Classe :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allClasses.forEach { cls ->
                            FilterChip(
                                selected = cls == selectedClass,
                                onClick = {
                                    selectedClass = cls
                                    if (classroom.isBlank() || classroom.startsWith("Salle")) {
                                        classroom = "Salle $cls"
                                    }
                                },
                                label = { Text(cls, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Day Choice
                item {
                    Text(text = "Jour de la semaine :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        days.forEach { d ->
                            FilterChip(
                                selected = d == selectedDay,
                                onClick = { selectedDay = d },
                                label = { Text(d, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Hours
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Début (HH:mm)") },
                            placeholder = { Text("08:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Fin (HH:mm)") },
                            placeholder = { Text("10:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Subject presets chip + input
                item {
                    Text(text = "Matière :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { subj ->
                            FilterChip(
                                selected = subjectName == subj.name,
                                onClick = {
                                    subjectName = subj.name
                                    teacherName = subj.teacherName
                                },
                                label = { Text(subj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Intitulé matière") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Teacher
                item {
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Enseignant responsable") },
                        placeholder = { Text("ex: M. Konaté, Mme Dupont") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Classroom Room number
                item {
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Lieu / Numéro de salle") },
                        placeholder = { Text("ex: Salle 12, Labo SVT") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Actions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onDelete != null) {
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EduRedAlert),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Supprimer", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (subjectName.isNotBlank() && teacherName.isNotBlank()) {
                                    onConfirm(
                                        initialSlot.copy(
                                            className = selectedClass,
                                            day = selectedDay,
                                            startTime = startTime.ifBlank { "08:00" },
                                            endTime = endTime.ifBlank { "10:00" },
                                            subjectName = subjectName.trim(),
                                            teacherName = teacherName.trim(),
                                            classroom = classroom.ifBlank { "Salle $selectedClass" }
                                        )
                                    )
                                }
                            },
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
