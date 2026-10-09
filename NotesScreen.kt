package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.model.Subject
import com.example.ui.components.TrimesterSelectorRow
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun NotesScreen(
    students: List<Student>,
    subjects: List<Subject>,
    selectedSubjectIndex: Int,
    selectedTrimester: Int,
    classAverage: Double,
    getGrade: (Int, Int, Int) -> Double,
    onGradeChanged: (Int, Int, Int, Double) -> Unit,
    onSubjectSelected: (Int) -> Unit,
    onTrimesterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubject = subjects.getOrElse(selectedSubjectIndex) { subjects.first() }
    var studentToEditGrade by remember { mutableStateOf<Student?>(null) }
    var customGradeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Subject Selector Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Matière & Enseignant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEachIndexed { index, subj ->
                        val isSelected = index == selectedSubjectIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectSelected(index) },
                            label = { Text(subj.name, fontSize = 12.sp) },
                            shape = RoundedCornerShape(99.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EduIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Header info card: Current subject, Teacher & Class average
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
                        Column {
                            Text(
                                text = "${currentSubject.name} (Coef ${currentSubject.coef})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enseignant : ${currentSubject.teacherName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduIndigoSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Moyenne classe",
                                    fontSize = 10.sp,
                                    color = EduIndigoDark
                                )
                                Text(
                                    text = "${String.format("%.2f", classAverage)}/20",
                                    fontWeight = FontWeight.Bold,
                                    color = EduIndigo,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TrimesterSelectorRow(
                        selectedTrimester = selectedTrimester,
                        onTrimesterSelected = onTrimesterSelected
                    )
                }
            }
        }

        // Grade Input List for each student
        item {
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Saisie des notes sur 20 (Trimestre $selectedTrimester)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    students.forEach { st ->
                        val currentGrade = getGrade(st.id, currentSubject.id, selectedTrimester)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Classe ${st.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Interactive score controls (+/- buttons, direct click to type & quick adjust)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newG = (currentGrade - 0.5).coerceAtLeast(0.0)
                                        onGradeChanged(st.id, currentSubject.id, selectedTrimester, newG)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuer note")
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentGrade >= 10.0) EduGreenLight else EduRedLight,
                                    modifier = Modifier
                                        .width(62.dp)
                                        .clickable {
                                            studentToEditGrade = st
                                            customGradeInput = String.format(Locale.US, "%.1f", currentGrade)
                                        }
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", currentGrade),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (currentGrade >= 10.0) EduGreenSuccess else EduRedAlert,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newG = (currentGrade + 0.5).coerceAtMost(20.0)
                                        onGradeChanged(st.id, currentSubject.id, selectedTrimester, newG)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Augmenter note")
                                }

                                IconButton(
                                    onClick = {
                                        studentToEditGrade = st
                                        customGradeInput = String.format(Locale.US, "%.1f", currentGrade)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Modifier la note directement",
                                        tint = EduIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }

    // Direct Grade Modification Dialog for Teachers
    if (studentToEditGrade != null) {
        val st = studentToEditGrade!!
        val currentG = getGrade(st.id, currentSubject.id, selectedTrimester)
        AlertDialog(
            onDismissRequest = { studentToEditGrade = null },
            title = {
                Text(
                    text = "Modifier la note • ${st.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Matière : ${currentSubject.name} (Trimestre $selectedTrimester)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customGradeInput,
                        onValueChange = { input ->
                            // Allow numbers and decimal point
                            if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                customGradeInput = input
                            }
                        },
                        label = { Text("Note sur 20") },
                        placeholder = { Text("ex: 14.5") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick presets
                    Text("Raccourcis de notation :", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(8.0, 10.0, 12.5, 15.0, 18.0).forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    customGradeInput = preset.toString()
                                },
                                label = { Text("${preset.toInt()}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customGradeInput.replace(',', '.').toDoubleOrNull()
                        if (parsed != null) {
                            val bounded = parsed.coerceIn(0.0, 20.0)
                            onGradeChanged(st.id, currentSubject.id, selectedTrimester, bounded)
                        }
                        studentToEditGrade = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduIndigo)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToEditGrade = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
