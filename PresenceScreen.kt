package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun PresenceScreen(
    attendanceList: List<StudentAttendance>,
    students: List<Student>,
    onToggleAttendance: (Int) -> Unit,
    onNotifyParents: () -> Unit,
    onContactParent: (Student) -> Unit,
    modifier: Modifier = Modifier
) {
    val absentCount = attendanceList.count { it.status == AttendanceStatus.ABSENT }
    val retardCount = attendanceList.count { it.status == AttendanceStatus.RETARD }
    val presentCount = attendanceList.count { it.status == AttendanceStatus.PRESENT }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Attendance Overview & Summary
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
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Registre d'appel & Présence",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EduIndigoSurface
                                ) {
                                    Text(
                                        text = "3e A",
                                        fontWeight = FontWeight.Bold,
                                        color = EduIndigo,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Collège & Lycée • Groupe Scolaire Lasme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onNotifyParents,
                            colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("notify_parents_button")
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alerter familles", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status counters chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduGreenLight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Présents", fontSize = 11.sp, color = EduGreenSuccess)
                                Text("$presentCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduGreenSuccess)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduRedLight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Absents", fontSize = 11.sp, color = EduRedAlert)
                                Text("$absentCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduRedAlert)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduAmberLight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Retards", fontSize = 11.sp, color = EduAmberWarn)
                                Text("$retardCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduAmberWarn)
                            }
                        }
                    }
                }
            }
        }

        // Automated alerts banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EduGreenLight.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EduGreenSuccess.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(EduGreenSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = EduGreenSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alertes automatisées actives",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EduGreenSuccess
                        )
                        Text(
                            text = "Chaque fois qu'un élève est marqué absent ou en retard, une notification instantanée et un SMS sont envoyés au parent.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Student Roll Call interactive list
        item {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Appel de la classe (Toucher un statut pour le modifier)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    attendanceList.forEach { att ->
                        val student = students.find { it.id == att.studentId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = att.studentName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Classe 3e A • Parent : ${student?.parentName ?: "Famille"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Attendance state chip button
                                FilledTonalButton(
                                    onClick = { onToggleAttendance(att.studentId) },
                                    shape = RoundedCornerShape(99.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = when (att.status) {
                                            AttendanceStatus.PRESENT -> EduGreenLight
                                            AttendanceStatus.ABSENT -> EduRedLight
                                            AttendanceStatus.RETARD -> EduAmberLight
                                        },
                                        contentColor = when (att.status) {
                                            AttendanceStatus.PRESENT -> EduGreenSuccess
                                            AttendanceStatus.ABSENT -> EduRedAlert
                                            AttendanceStatus.RETARD -> EduAmberWarn
                                        }
                                    ),
                                    modifier = Modifier.testTag("attendance_btn_${att.studentId}")
                                ) {
                                    Text(
                                        text = "${att.status.label} (${att.status.code})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (student != null) {
                                    IconButton(
                                        onClick = { onContactParent(student) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "Message direct parent",
                                            tint = EduIndigo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}
