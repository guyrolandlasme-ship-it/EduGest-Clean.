package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.data.repository.EduGestRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParentViewScreen(
    currentSubTab: String, // "Bulletin", "Paiements", "Absences"
    students: List<Student>,
    selectedStudentId: Int,
    selectedTrimester: Int,
    reportCard: StudentReportCard?,
    receipts: List<PaymentReceiptItem>,
    attendanceList: List<StudentAttendance>,
    onStudentSelected: (Int) -> Unit,
    onTrimesterSelected: (Int) -> Unit,
    onContactTeacher: () -> Unit,
    onProcessInstantPayment: ((Int, Long, String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentStudent = students.find { it.id == selectedStudentId }
    val studentReceipts = receipts.filter { it.studentId == selectedStudentId }
    val studentAttendance = attendanceList.find { it.studentId == selectedStudentId }

    // Instant Payment states
    var selectedProvider by remember { mutableStateOf("Wave") } // "Wave" or "Orange Money"
    var selectedAmountPreset by remember { mutableStateOf<Long?>(50_000L) }
    var customAmountText by remember { mutableStateOf("") }
    var payerPhoneNumber by remember { mutableStateOf("+225 07 12 34 56") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var latestValidatedReceipt by remember { mutableStateOf<PaymentReceiptItem?>(null) }
    var viewingReceiptDetail by remember { mutableStateOf<PaymentReceiptItem?>(null) }

    val effectiveAmount: Long = selectedAmountPreset ?: (customAmountText.toLongOrNull() ?: 50_000L)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Child selector
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
                        Text(
                            text = "Enfant sélectionné :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StudentDropdownSelector(
                            students = students,
                            selectedStudentId = selectedStudentId,
                            onStudentSelected = onStudentSelected
                        )
                    }
                }
            }
        }

        when (currentSubTab) {
            "Bulletin", "Mon Bulletin" -> {
                // Trimester tabs
                item {
                    TrimesterSelectorRow(
                        selectedTrimester = selectedTrimester,
                        onTrimesterSelected = onTrimesterSelected
                    )
                }

                // Official Report Card Header & Metrics
                item {
                    if (reportCard != null) {
                        OutlinedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Bulletin Scolaire Officiel • Trimestre $selectedTrimester",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Élève : ${reportCard.student.name} • Classe de 3e A",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                KpiGrid(
                                    items = listOf(
                                        KpiItem("Moyenne", String.format(Locale.US, "%.2f", reportCard.overallAverage) + "/20", "Trimestre $selectedTrimester", Icons.Default.Analytics, EduIndigo),
                                        KpiItem("Rang", "${reportCard.rank}e / ${reportCard.totalStudents}", "Classe de 3e A", Icons.Default.MilitaryTech, EduGold),
                                        KpiItem("Mention", reportCard.mention, "Conseil de classe", Icons.Default.Grade, EduGreenSuccess)
                                    )
                                )
                            }
                        }
                    }
                }

                // Detailed Grades List
                item {
                    if (reportCard != null) {
                        OutlinedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Détail par discipline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                reportCard.subjectGrades.forEach { detail ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = detail.subject.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Coef ${detail.subject.coef} • Prof : ${detail.subject.teacherName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format(Locale.US, "%.2f", detail.score) + " / 20",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (detail.score >= 10.0) EduGreenSuccess else EduRedAlert
                                            )
                                            Text(
                                                text = "Moy. classe : " + String.format(Locale.US, "%.2f", detail.classAverage),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Anti-Fraud QR Code Authenticity Section
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EduIndigo.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.size(50.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode2,
                                                    contentDescription = "QR Code Anti-Fraude",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = EduGreenSuccess, modifier = Modifier.size(13.dp))
                                                Text("Authentification QR Sécurisée", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EduIndigo)
                                            }
                                            Text("Certifié par la Direction GROUPE SCOLAIRE LASME", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "Matricule : ${reportCard.student.matricule.ifBlank { "MAT-2026-${reportCard.student.id}" }} • SHA-256 #9F8A-${reportCard.student.id}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Téléchargement du Bulletin Officiel T${reportCard.trimester} (PDF certifié par QR Code)", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Télécharger Bulletin Officiel (PDF signé)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Contact Teacher CTA
                item {
                    Button(
                        onClick = onContactTeacher,
                        colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_teacher_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Échanger avec le Professeur Principal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            "Paiements" -> {
                // Tuition balance KPI Cards
                item {
                    if (currentStudent != null) {
                        KpiGrid(
                            items = listOf(
                                KpiItem("Frais annuels", "${EduGestRepository.formatAmount(currentStudent.tuitionFee)} F", "3e A", Icons.Default.School, EduIndigo),
                                KpiItem("Payé", "${EduGestRepository.formatAmount(currentStudent.paidAmount)} F", "Validé", Icons.Default.CheckCircle, EduGreenSuccess),
                                KpiItem("Reste", "${EduGestRepository.formatAmount(currentStudent.remainingBalance)} F", "À solder", Icons.Default.Schedule, if (currentStudent.remainingBalance == 0L) EduGreenSuccess else EduRedAlert)
                            )
                        )
                    }
                }

                // Instant Payment Module (Wave & Orange Money)
                item {
                    val remaining = currentStudent?.remainingBalance ?: 0L
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color(0xFFFF7900)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instant_payment_module")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Section header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color(0xFFFF7900),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Paiement Instantané",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Règlement mobile sécurisé avec reçu immédiat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedProvider == "Wave") Color(0xFFE1F5FE) else Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = if (selectedProvider == "Wave") "0% frais" else "Sécurisé",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedProvider == "Wave") Color(0xFF0288D1) else Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Provider selector: Wave vs Orange Money
                            Text(
                                text = "1. Choisissez votre moyen de paiement :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Wave Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (selectedProvider == "Wave") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0D47A1)) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedProvider = "Wave" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (selectedProvider == "Wave") Color.White else Color(0xFF1BA0E2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("W", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Wave",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (selectedProvider == "Wave") Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Côte d'Ivoire",
                                                fontSize = 10.sp,
                                                color = if (selectedProvider == "Wave") Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Orange Money Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedProvider == "Orange Money") Color(0xFFFF7900) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (selectedProvider == "Orange Money") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFBF360C)) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedProvider = "Orange Money" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (selectedProvider == "Orange Money") Color.Black else Color(0xFFFF7900)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("OM", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Orange Money",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (selectedProvider == "Orange Money") Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Validation PIN",
                                                fontSize = 10.sp,
                                                color = if (selectedProvider == "Orange Money") Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Amount Selection
                            Text(
                                text = "2. Montant à verser :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(50_000L, 100_000L).forEach { amt ->
                                    val isSel = selectedAmountPreset == amt
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedAmountPreset = amt
                                            customAmountText = ""
                                        },
                                        label = { Text("${EduGestRepository.formatAmount(amt)} F", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                if (remaining > 0L) {
                                    val isSel = selectedAmountPreset == remaining
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedAmountPreset = remaining
                                            customAmountText = ""
                                        },
                                        label = { Text("Solder (${EduGestRepository.formatAmount(remaining)} F)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom amount field
                            OutlinedTextField(
                                value = if (selectedAmountPreset != null) EduGestRepository.formatAmount(selectedAmountPreset!!) else customAmountText,
                                onValueChange = {
                                    selectedAmountPreset = null
                                    customAmountText = it.filter { char -> char.isDigit() }
                                },
                                label = { Text("Montant sélectionné (FCFA)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Payer Phone Number
                            OutlinedTextField(
                                value = payerPhoneNumber,
                                onValueChange = { payerPhoneNumber = it },
                                label = { Text("Numéro de téléphone $selectedProvider") },
                                placeholder = { Text("+225 07 00 00 00 00") },
                                leadingIcon = {
                                    Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive QR Code for Validation
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "QR Code de Validation Instantanée",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Custom rendered QR code pattern with central provider logo
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Stylized QR pattern
                                        StyledQrCodeCanvas(
                                            providerColor = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color(0xFFFF7900)
                                        )

                                        // Central logo pill
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color(0xFFFF7900),
                                            shadowElevation = 2.dp
                                        ) {
                                            Text(
                                                text = if (selectedProvider == "Wave") "Wave" else "OM",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Scannez avec votre application $selectedProvider ou validez directement via le bouton ci-dessous",
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Validate Payment CTA
                            Button(
                                onClick = {
                                    if (effectiveAmount > 0L) {
                                        isProcessingPayment = true
                                        onProcessInstantPayment?.invoke(
                                            selectedStudentId,
                                            effectiveAmount,
                                            selectedProvider,
                                            payerPhoneNumber
                                        )
                                        val generatedRec = PaymentReceiptItem(
                                            receiptNumber = "REC-${selectedProvider.take(2).uppercase()}-${System.currentTimeMillis() % 1000000}",
                                            studentId = selectedStudentId,
                                            studentName = currentStudent?.name ?: "Élève 3e A",
                                            amount = effectiveAmount,
                                            timestamp = System.currentTimeMillis(),
                                            cashierName = "Paiement Instantané $selectedProvider",
                                            paymentMethod = selectedProvider
                                        )
                                        latestValidatedReceipt = generatedRec
                                        isProcessingPayment = false
                                    }
                                },
                                enabled = !isProcessingPayment && effectiveAmount > 0L,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedProvider == "Wave") Color(0xFF1BA0E2) else Color(0xFFFF7900)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("validate_instant_payment_btn")
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validation en cours...")
                                } else {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Valider ${EduGestRepository.formatAmount(effectiveAmount)} FCFA ($selectedProvider)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Receipts History
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
                                Text(
                                    text = "Historique officiel des reçus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduGreenLight
                                ) {
                                    Text(
                                        text = "${studentReceipts.size} reçus",
                                        fontWeight = FontWeight.Bold,
                                        color = EduGreenSuccess,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            if (studentReceipts.isEmpty()) {
                                Text(
                                    text = "Aucun versement enregistré pour le moment.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                studentReceipts.forEach { rec ->
                                    val dateFmt = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewingReceiptDetail = rec }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                rec.paymentMethod.contains("Wave", ignoreCase = true) -> Color(0xFF1BA0E2)
                                                                rec.paymentMethod.contains("Orange", ignoreCase = true) -> Color(0xFFFF7900)
                                                                else -> EduGreenSuccess
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = "Reçu ${rec.receiptNumber}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "${rec.paymentMethod} • $dateFmt",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${EduGestRepository.formatAmount(rec.amount)} FCFA",
                                                    fontWeight = FontWeight.Bold,
                                                    color = EduGreenSuccess,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Voir le reçu",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = EduIndigo
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Absences", "Assiduité" -> {
                // Attendance tracking for this child
                item {
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "État des présences & Assiduité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Statut aujourd'hui :",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val status = studentAttendance?.status ?: AttendanceStatus.PRESENT
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (status) {
                                        AttendanceStatus.PRESENT -> EduGreenLight
                                        AttendanceStatus.ABSENT -> EduRedLight
                                        AttendanceStatus.RETARD -> EduAmberLight
                                    }
                                ) {
                                    Text(
                                        text = status.label,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            AttendanceStatus.PRESENT -> EduGreenSuccess
                                            AttendanceStatus.ABSENT -> EduRedAlert
                                            AttendanceStatus.RETARD -> EduAmberWarn
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Absence notification info
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EduIndigo, modifier = Modifier.size(20.dp))
                                Text("Avis aux familles", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toute absence doit être justifiée dans les 48h auprès du bureau de la vie scolaire (Éducateur).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onContactTeacher,
                        colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transmettre un justificatif d'absence", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal Receipt View (upon instant validation or clicking receipt in history)
    val receiptToDisplay = latestValidatedReceipt ?: viewingReceiptDetail
    if (receiptToDisplay != null) {
        OfficialReceiptDialog(
            receipt = receiptToDisplay,
            student = currentStudent,
            onDismiss = {
                latestValidatedReceipt = null
                viewingReceiptDetail = null
            }
        )
    }
}

@Composable
fun StyledQrCodeCanvas(providerColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val s = size.width
        val blockSize = s / 15f

        // Draw background white
        drawRect(Color.White)

        // Corner Finder Patterns (Top-Left, Top-Right, Bottom-Left)
        fun drawFinder(x: Float, y: Float) {
            drawRect(Color.Black, topLeft = Offset(x, y), size = Size(blockSize * 4, blockSize * 4))
            drawRect(Color.White, topLeft = Offset(x + blockSize, y + blockSize), size = Size(blockSize * 2, blockSize * 2))
            drawRect(providerColor, topLeft = Offset(x + blockSize * 1.5f, y + blockSize * 1.5f), size = Size(blockSize, blockSize))
        }

        drawFinder(0f, 0f)
        drawFinder(s - blockSize * 4, 0f)
        drawFinder(0f, s - blockSize * 4)

        // Matrix dots
        for (i in 0..14) {
            for (j in 0..14) {
                // skip corners
                if ((i < 4 && j < 4) || (i > 10 && j < 4) || (i < 4 && j > 10)) continue
                // skip center logo
                if (i in 5..9 && j in 5..9) continue

                if ((i * 3 + j * 7 + (i * j) % 5) % 3 == 0) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset(i * blockSize, j * blockSize),
                        size = Size(blockSize * 0.85f, blockSize * 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun OfficialReceiptDialog(
    receipt: PaymentReceiptItem,
    student: Student?,
    onDismiss: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd MMMM yyyy à HH:mm", Locale.getDefault()).format(Date(receipt.timestamp))

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
                    .padding(20.dp)
            ) {
                // Header with official badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GROUPE SCOLAIRE LASME",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EduIndigo
                        )
                        Text(
                            text = "Reçu Officiel d'Encaissement",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Receipt Content
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF9F9FB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("N° de reçu :", fontSize = 11.sp, color = Color.Gray)
                            Text(receipt.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EduIndigo)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date & Heure :", fontSize = 11.sp, color = Color.Gray)
                            Text(dateFmt, fontSize = 11.sp, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Élève :", fontSize = 11.sp, color = Color.Gray)
                            Text(receipt.studentName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Classe :", fontSize = 11.sp, color = Color.Gray)
                            Text(student?.className ?: "3e A", fontSize = 11.sp, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode de règlement :", fontSize = 11.sp, color = Color.Gray)
                            Text(receipt.paymentMethod, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = EduGreenSuccess)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MONTANT VERSÉ :", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text(
                                text = "${EduGestRepository.formatAmount(receipt.amount)} FCFA",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = EduGreenSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stamp / Cachet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, EduGreenSuccess),
                                color = EduGreenLight.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = EduGreenSuccess, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "PAIEMENT VALIDÉ EN COMPTABILITÉ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = EduGreenSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terminé", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
