package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.*
import com.example.data.repository.EduGestRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FinancesScreen(
    currentRole: UserRole = UserRole.CAISSE,
    students: List<Student>,
    receipts: List<PaymentReceiptItem>,
    onProcessPayment: (Int, Long, String) -> Unit,
    onContactParent: (Student) -> Unit,
    onToggleAccessSuspension: ((Int, Boolean) -> Unit)? = null,
    onToggleDirectorWaiver: ((Int, Boolean) -> Unit)? = null,
    onSendTuitionReminder: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedStudentId by remember { mutableStateOf(0) }
    var paymentAmountText by remember { mutableStateOf("50000") }
    var paymentMethod by remember { mutableStateOf("Wave") }
    var selectedReceiptForDialog by remember { mutableStateOf<PaymentReceiptItem?>(null) }

    val totalExpected = students.sumOf { it.tuitionFee }
    val totalCollected = students.sumOf { it.paidAmount }
    val totalRemaining = (totalExpected - totalCollected).coerceAtLeast(0L)

    val currentStudent = students.find { it.id == selectedStudentId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Financial KPIs
        item {
            KpiGrid(
                items = listOf(
                    KpiItem("Attendu", "${EduGestRepository.formatAmount(totalExpected)} F", "3e A global", Icons.Default.AccountBalance, EduIndigo),
                    KpiItem("Encaissé", "${EduGestRepository.formatAmount(totalCollected)} F", "Recouvrement", Icons.Default.Payments, EduGreenSuccess),
                    KpiItem("Reste", "${EduGestRepository.formatAmount(totalRemaining)} F", "À percevoir", Icons.Default.PendingActions, EduRedAlert)
                )
            )
        }

        // Cashiering Action Card
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
                        Text(
                            text = "Opération de caisse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduGreenSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Encaissement direct",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduGreenSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StudentDropdownSelector(
                        students = students,
                        selectedStudentId = selectedStudentId,
                        onStudentSelected = { selectedStudentId = it },
                        label = "Élève concerné"
                    )

                    if (currentStudent != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Déjà versé : ${EduGestRepository.formatAmount(currentStudent.paidAmount)} FCFA",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Reste : ${EduGestRepository.formatAmount(currentStudent.remainingBalance)} FCFA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentStudent.remainingBalance == 0L) EduGreenSuccess else EduRedAlert
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Montant à encaisser (FCFA)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_amount_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Quick presets chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50_000L, 100_000L, 150_000L).forEach { preset ->
                            SuggestionChip(
                                onClick = { paymentAmountText = preset.toString() },
                                label = { Text("+${EduGestRepository.formatAmount(preset)} F", fontSize = 11.sp) }
                            )
                        }
                        if (currentStudent != null && currentStudent.remainingBalance > 0) {
                            SuggestionChip(
                                onClick = { paymentAmountText = currentStudent.remainingBalance.toString() },
                                label = { Text("Solde total", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Payment method (Wave, Orange Money, MTN, Moov, Espèces)
                    Text("Mode de règlement :", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Wave", "Orange Money", "MTN Moov", "Espèces", "Virement").forEach { method ->
                            val isSel = method == paymentMethod
                            FilterChip(
                                selected = isSel,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (method.contains("Wave") || method.contains("Orange")) EduGreenSuccess else EduIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (paymentMethod in listOf("Wave", "Orange Money", "MTN Moov")) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduGreenLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = EduGreenSuccess, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Règlement Mobile Money : Déblocage automatique immédiat des cours et devoirs PDF.",
                                    fontSize = 11.sp,
                                    color = EduGreenSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amount = paymentAmountText.toLongOrNull() ?: 0L
                            if (amount > 0) {
                                onProcessPayment(selectedStudentId, amount, paymentMethod)
                            }
                        },
                        enabled = (paymentAmountText.toLongOrNull() ?: 0L) > 0 &&
                                (currentStudent?.remainingBalance ?: 0L) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_payment_button")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encaisser & Émettre le reçu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Students Tuition Ledger Table with Access Control
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
                                text = "Contrôle d'accès & Recouvrement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Blocage/déblocage cours en ligne & devoirs PDF selon paiement",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    students.forEach { st ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = st.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EduIndigo.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = st.matricule.ifBlank { "MAT-${st.id}" },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EduIndigo,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(text = "• ${st.className}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Payé : ${EduGestRepository.formatAmount(st.paidAmount)} / ${EduGestRepository.formatAmount(st.tuitionFee)} FCFA • Reste : ${EduGestRepository.formatAmount(st.remainingBalance)} F",
                                        fontSize = 11.sp,
                                        color = if (st.remainingBalance == 0L) EduGreenSuccess else EduRedAlert
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Financial Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (st.paymentStatus) {
                                            PaymentStatus.SOLDE -> EduGreenLight
                                            PaymentStatus.PARTIEL -> EduAmberLight
                                            PaymentStatus.IMPAYE -> EduRedLight
                                        }
                                    ) {
                                        Text(
                                            text = st.paymentStatus.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (st.paymentStatus) {
                                                PaymentStatus.SOLDE -> EduGreenSuccess
                                                PaymentStatus.PARTIEL -> EduAmberWarn
                                                PaymentStatus.IMPAYE -> EduRedAlert
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Access Status Bar & Action Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Access Pill
                                if (st.isAccessSuspended && !st.directorWaiver) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = EduRedLight) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = EduRedAlert, modifier = Modifier.size(12.dp))
                                            Text("Cours & PDF bloqués", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EduRedAlert)
                                        }
                                    }
                                } else if (st.directorWaiver) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF3E8FF)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(12.dp))
                                            Text("Dérogation Directeur", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                        }
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = EduGreenLight) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EduGreenSuccess, modifier = Modifier.size(12.dp))
                                            Text("Accès actif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EduGreenSuccess)
                                        }
                                    }
                                }

                                // Interactive Action Buttons: Suspension, Waiver, Relance WhatsApp
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Relance Parent (Notification + Lien Mobile Money)
                                    if (st.remainingBalance > 0) {
                                        IconButton(
                                            onClick = {
                                                onSendTuitionReminder?.invoke(st.id)
                                                if (st.whatsappNumber.isNotBlank()) {
                                                    try {
                                                        val clean = st.whatsappNumber.replace("+", "").replace(" ", "").replace("-", "")
                                                        val msg = "GROUPE SCOLAIRE LASME : Rappel scolarité pour ${st.name}. Reste à payer : ${EduGestRepository.formatAmount(st.remainingBalance)} FCFA. Réglez en 1 clic par Wave/Orange/MTN/Moov : https://pay.edugest.ci/lasme?studentId=${st.id}"
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${URLEncoder.encode(msg, "UTF-8")}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Relance enregistrée", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.NotificationsActive,
                                                contentDescription = "Relance Parent avec lien",
                                                tint = Color(0xFF25D366),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Comptable / Direction Toggle Suspension
                                    IconButton(
                                        onClick = {
                                            onToggleAccessSuspension?.invoke(st.id, !st.isAccessSuspended)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (st.isAccessSuspended) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = if (st.isAccessSuspended) "Débloquer l'accès" else "Suspendre l'accès",
                                            tint = if (st.isAccessSuspended) EduGreenSuccess else EduRedAlert,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Directeur Waiver Right
                                    if (currentRole == UserRole.DIRECTION) {
                                        IconButton(
                                            onClick = {
                                                onToggleDirectorWaiver?.invoke(st.id, !st.directorWaiver)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = "Droit de dérogation Directeur",
                                                tint = Color(0xFF7E22CE),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Chat shortcut
                                    IconButton(
                                        onClick = { onContactParent(st) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "Contacter le parent",
                                            tint = EduIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        // Receipts Ledger
        item {
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Reçus émis (${receipts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (receipts.isEmpty()) {
                        Text(
                            text = "Aucun reçu généré pour le moment.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        receipts.forEach { rec ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedReceiptForDialog = rec }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${rec.receiptNumber} • ${rec.studentName}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${rec.paymentMethod} • ${rec.cashierName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${EduGestRepository.formatAmount(rec.amount)} F",
                                            fontWeight = FontWeight.Bold,
                                            color = EduIndigo,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Voir reçu",
                                            fontSize = 10.sp,
                                            color = EduSky
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

    // Modal Receipt Inspection Dialog
    if (selectedReceiptForDialog != null) {
        val r = selectedReceiptForDialog!!
        val dateFmt = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault()).format(Date(r.timestamp))
        Dialog(onDismissRequest = { selectedReceiptForDialog = null }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLLÈGE HORIZON",
                            fontWeight = FontWeight.Bold,
                            color = EduIndigo,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { selectedReceiptForDialog = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                    Text(
                        text = "Reçu officiel de paiement de scolarité",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(text = "Numéro : ${r.receiptNumber}", fontWeight = FontWeight.Bold)
                    Text(text = "Élève : ${r.studentName} (3e A)")
                    Text(text = "Date : $dateFmt")
                    Text(text = "Mode de règlement : ${r.paymentMethod}")
                    Text(text = "Caisse : ${r.cashierName}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = EduIndigoSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "MONTANT ENCAISSÉ", fontSize = 11.sp, color = EduIndigoDark)
                            Text(
                                text = "${EduGestRepository.formatAmount(r.amount)} FCFA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = EduIndigo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { selectedReceiptForDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer le reçu")
                    }
                }
            }
        }
    }
}
