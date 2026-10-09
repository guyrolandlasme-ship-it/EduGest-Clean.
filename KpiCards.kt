package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.theme.*

data class KpiItem(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val color: Color = EduIndigo
)

@Composable
fun KpiGrid(
    items: List<KpiItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { kpi ->
            OutlinedCard(
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = kpi.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = kpi.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = kpi.color,
                        fontSize = 17.sp
                    )
                    if (kpi.subtitle != null) {
                        Text(
                            text = kpi.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDropdownSelector(
    students: List<Student>,
    selectedStudentId: Int,
    onStudentSelected: (Int) -> Unit,
    label: String = "Sélectionner un élève",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStudent = students.find { it.id == selectedStudentId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentStudent?.let { "${it.name} (${it.className})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("student_dropdown_field"),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            students.forEach { student ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = student.name,
                                fontWeight = if (student.id == selectedStudentId) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Classe ${student.className} • ${student.parentName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onStudentSelected(student.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("student_item_${student.id}")
                )
            }
        }
    }
}

@Composable
fun TrimesterSelectorRow(
    selectedTrimester: Int,
    onTrimesterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 2, 3).forEach { t ->
            val isSelected = t == selectedTrimester
            FilledTonalButton(
                onClick = { onTrimesterSelected(t) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) EduIndigo else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("trimester_button_$t")
            ) {
                Text(
                    text = "Trimestre $t",
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
