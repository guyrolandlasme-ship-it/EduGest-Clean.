package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.KpiGrid
import com.example.ui.components.KpiItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserManagementScreen(
    users: List<UserAccount>,
    students: List<Student>,
    subjects: List<Subject>,
    onCreateAccount: (String, String, String, UserRole, String, String, Int?, Int?, String, String, String, String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    val filteredUsers = users.filter { u ->
        val matchesRole = selectedRoleFilter == null || u.role == selectedRoleFilter
        val matchesSearch = u.fullName.contains(searchQuery, ignoreCase = true) ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.role.displayName.contains(searchQuery, ignoreCase = true) ||
                u.studentClass.contains(searchQuery, ignoreCase = true) ||
                u.controlledClasses.contains(searchQuery, ignoreCase = true) ||
                u.taughtSubjects.contains(searchQuery, ignoreCase = true) ||
                u.attachedStudents.contains(searchQuery, ignoreCase = true)
        matchesRole && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Header & Summary
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
                            Text(
                                text = "Gestion des accès & Profils",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Administration des attributions et identifiants officiels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("create_account_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Créer compte", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Metrics Grid
        item {
            KpiGrid(
                items = listOf(
                    KpiItem("Total comptes", "${users.size}", "Actifs", Icons.Default.People, EduIndigo),
                    KpiItem("Enseignants", "${users.count { it.role == UserRole.ENSEIGNANT }}", "Corps professoral", Icons.Default.School, EduSky),
                    KpiItem("Éducateurs", "${users.count { it.role == UserRole.EDUCATEUR }}", "Vie scolaire", Icons.Default.Security, EduAmberWarn)
                )
            )
        }

        // Search Bar & Role Filter Chips
        item {
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un compte, classe, matière...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedRoleFilter == null,
                            onClick = { selectedRoleFilter = null },
                            label = { Text("Tous (${users.size})", fontSize = 11.sp) }
                        )

                        listOf(
                            UserRole.ENSEIGNANT,
                            UserRole.EDUCATEUR,
                            UserRole.CAISSE,
                            UserRole.PARENT,
                            UserRole.ELEVE,
                            UserRole.DIRECTION
                        ).forEach { role ->
                            val count = users.count { it.role == role }
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = {
                                    selectedRoleFilter = if (selectedRoleFilter == role) null else role
                                },
                                label = { Text("${role.displayName} ($count)", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Users List
        item {
            Text(
                text = "Comptes enregistrés (${filteredUsers.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(filteredUsers, key = { it.id }) { user ->
            val dateFmt = remember(user.createdAt) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(user.createdAt))
            }

            val roleColor = when (user.role) {
                UserRole.DIRECTION -> EduIndigo
                UserRole.ENSEIGNANT -> EduSky
                UserRole.EDUCATEUR -> EduAmberWarn
                UserRole.CAISSE -> EduGreenSuccess
                UserRole.PARENT -> EduGold
                UserRole.ELEVE -> EduIndigoDark
            }

            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (user.role) {
                                UserRole.DIRECTION -> Icons.Default.AdminPanelSettings
                                UserRole.ENSEIGNANT -> Icons.Default.School
                                UserRole.EDUCATEUR -> Icons.Default.Security
                                UserRole.CAISSE -> Icons.Default.AccountBalanceWallet
                                UserRole.PARENT -> Icons.Default.FamilyRestroom
                                UserRole.ELEVE -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // User Details
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = roleColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = user.role.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = roleColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Code d'accès : ${user.username} • Mot de passe : ${user.password}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mandatory Profile Attributions display
                        when (user.role) {
                            UserRole.ELEVE -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EduIndigoSurface,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Classe : ${user.studentClass.ifEmpty { user.className }}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduIndigo,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            UserRole.EDUCATEUR -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EduAmberLight,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Classe(s) contrôlée(s) : ${user.controlledClasses}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduAmberWarn,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            UserRole.ENSEIGNANT -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EduSky.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Matières dispensées : ${user.taughtSubjects}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduSky,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            UserRole.PARENT -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EduGoldLight,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Élève(s) rattaché(s) : ${user.attachedStudents}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            else -> {}
                        }

                        Text(
                            text = "Créé le $dateFmt • Statut : Actif",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (user.role != UserRole.DIRECTION) {
                        IconButton(
                            onClick = { userToDelete = user },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Révoquer l'accès",
                                tint = EduRedAlert,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to create user account with MANDATORY fields
    if (showCreateDialog) {
        CreateAccountWithAttributionsDialog(
            students = students,
            subjects = subjects,
            onConfirm = { name, uName, pass, role, email, phone, stId, subjId, stClass, ctrlClasses, taughtSubjs, attachedSts ->
                onCreateAccount(name, uName, pass, role, email, phone, stId, subjId, stClass, ctrlClasses, taughtSubjs, attachedSts)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Révoquer l'accès ?") },
            text = { Text("Le compte de ${userToDelete?.fullName} (${userToDelete?.username}) sera supprimé définitivement.") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.id?.let { onDeleteAccount(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduRedAlert)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountWithAttributionsDialog(
    students: List<Student>,
    subjects: List<Subject>,
    onConfirm: (String, String, String, UserRole, String, String, Int?, Int?, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val allClasses = listOf("6e 1", "6e A", "5e A", "4e A", "3e A", "2nde C", "1ère D", "Tle D")

    var selectedRole by remember { mutableStateOf(UserRole.ENSEIGNANT) }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("lasme2026") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Mandatory attribution fields
    var studentClass by remember { mutableStateOf("3e A") }
    var controlledClasses by remember { mutableStateOf("3e A, 4e A, 5e A") }
    var taughtSubjects by remember { mutableStateOf("Mathématiques") }
    var attachedStudents by remember { mutableStateOf(students.firstOrNull()?.name ?: "Awa Diallo") }

    var selectedStudentId by remember { mutableStateOf(0) }
    var selectedSubjectId by remember { mutableStateOf(0) }

    var validationError by remember { mutableStateOf<String?>(null) }

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
                            text = "Nouveau Profil Utilisateur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                    Text(
                        text = "Ajout obligatoire des attributions selon le profil",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (validationError != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduRedLight
                        ) {
                            Text(
                                text = validationError!!,
                                color = EduRedAlert,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Role selection chips
                item {
                    Text(text = "Rôle de l'utilisateur :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            UserRole.DIRECTION,
                            UserRole.ENSEIGNANT,
                            UserRole.CAISSE,
                            UserRole.EDUCATEUR,
                            UserRole.PARENT,
                            UserRole.ELEVE
                        ).forEach { role ->
                            val isSel = role == selectedRole
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedRole = role
                                    validationError = null
                                    if (fullName.isNotBlank()) {
                                        val clean = fullName.trim().lowercase().replace(" ", ".")
                                        username = when (role) {
                                            UserRole.DIRECTION -> "dir.$clean"
                                            UserRole.ENSEIGNANT -> "prof.$clean"
                                            UserRole.EDUCATEUR -> "educ.$clean"
                                            UserRole.CAISSE -> "compta.$clean"
                                            UserRole.PARENT -> "parent.$clean"
                                            UserRole.ELEVE -> "eleve.$clean"
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        when (role) {
                                            UserRole.DIRECTION -> "Directeur"
                                            UserRole.CAISSE -> "Comptable"
                                            else -> role.displayName
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Full Name
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (it.isNotBlank()) {
                                val clean = it.trim().lowercase().replace(" ", ".")
                                username = when (selectedRole) {
                                    UserRole.DIRECTION -> "dir.$clean"
                                    UserRole.ENSEIGNANT -> "prof.$clean"
                                    UserRole.EDUCATEUR -> "educ.$clean"
                                    UserRole.CAISSE -> "compta.$clean"
                                    UserRole.PARENT -> "parent.$clean"
                                    UserRole.ELEVE -> "eleve.$clean"
                                }
                            }
                        },
                        label = { Text("Nom complet *") },
                        placeholder = { Text("ex: M. Kouamé, Mariam Sy") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Username / Identifiant
                item {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Identifiant / Code d'accès *") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Password
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe / Code PIN *") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // MANDATORY FIELD: Élève -> « Classe »
                if (selectedRole == UserRole.ELEVE) {
                    item {
                        Text(
                            text = "Classe de l'élève (Obligatoire) * :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduIndigo
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allClasses.forEach { cls ->
                                FilterChip(
                                    selected = studentClass == cls,
                                    onClick = { studentClass = cls },
                                    label = { Text(cls, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = studentClass,
                            onValueChange = { studentClass = it },
                            label = { Text("Classe confirmée *") },
                            placeholder = { Text("ex: 3e A") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // MANDATORY FIELD: Éducateur -> « Classe(s) contrôlée(s) »
                if (selectedRole == UserRole.EDUCATEUR) {
                    item {
                        Text(
                            text = "Classe(s) contrôlée(s) (Obligatoire) * :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduAmberWarn
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("6e A, 5e A, 4e A, 3e A", "2nde C, 1ère D, Tle D", "Toutes les classes").forEach { preset ->
                                FilterChip(
                                    selected = controlledClasses == preset,
                                    onClick = { controlledClasses = preset },
                                    label = { Text(preset, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = controlledClasses,
                            onValueChange = { controlledClasses = it },
                            label = { Text("Saisir ou ajuster les classes contrôlées *") },
                            placeholder = { Text("ex: 6e A, 5e A, 4e A, 3e A") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // MANDATORY FIELD: Professeur -> « Matières dispensées »
                if (selectedRole == UserRole.ENSEIGNANT) {
                    item {
                        Text(
                            text = "Matières dispensées (Obligatoire) * :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduSky
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjects.forEach { subj ->
                                val isSel = taughtSubjects.contains(subj.name)
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        taughtSubjects = if (isSel) {
                                            taughtSubjects.replace(subj.name, "").replace(", ,", ",").trim(',', ' ')
                                        } else {
                                            if (taughtSubjects.isBlank()) subj.name else "$taughtSubjects, ${subj.name}"
                                        }
                                        selectedSubjectId = subj.id
                                    },
                                    label = { Text(subj.name, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = taughtSubjects,
                            onValueChange = { taughtSubjects = it },
                            label = { Text("Matières dispensées confirmées *") },
                            placeholder = { Text("ex: Mathématiques, Sciences Physiques") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // MANDATORY FIELD: Parent -> « Élève(s) rattaché(s) »
                if (selectedRole == UserRole.PARENT) {
                    item {
                        Text(
                            text = "Élève(s) rattaché(s) (Obligatoire) * :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduGold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            students.forEach { st ->
                                val isSel = attachedStudents.contains(st.name)
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        attachedStudents = if (isSel) {
                                            attachedStudents.replace(st.name, "").replace(", ,", ",").trim(',', ' ')
                                        } else {
                                            if (attachedStudents.isBlank()) st.name else "$attachedStudents, ${st.name}"
                                        }
                                        selectedStudentId = st.id
                                    },
                                    label = { Text(st.name, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = attachedStudents,
                            onValueChange = { attachedStudents = it },
                            label = { Text("Nom(s) des élèves rattachés *") },
                            placeholder = { Text("ex: Awa Diallo, Karim Traoré") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Optional Email & Phone
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email de contact (optionnel)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone (optionnel)") },
                        placeholder = { Text("+225 ...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Action Buttons
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
                                // Validate mandatory attributes
                                when {
                                    fullName.isBlank() -> {
                                        validationError = "Le nom complet est obligatoire."
                                    }
                                    username.isBlank() -> {
                                        validationError = "L'identifiant est obligatoire."
                                    }
                                    password.isBlank() -> {
                                        validationError = "Le mot de passe est obligatoire."
                                    }
                                    selectedRole == UserRole.ELEVE && studentClass.isBlank() -> {
                                        validationError = "La rubrique « Classe » est obligatoire pour un élève."
                                    }
                                    selectedRole == UserRole.EDUCATEUR && controlledClasses.isBlank() -> {
                                        validationError = "La rubrique « Classe(s) contrôlée(s) » est obligatoire pour un éducateur."
                                    }
                                    selectedRole == UserRole.ENSEIGNANT && taughtSubjects.isBlank() -> {
                                        validationError = "La rubrique « Matières dispensées » est obligatoire pour un professeur."
                                    }
                                    selectedRole == UserRole.PARENT && attachedStudents.isBlank() -> {
                                        validationError = "La rubrique « Élève(s) rattaché(s) » est obligatoire pour un parent."
                                    }
                                    else -> {
                                        onConfirm(
                                            fullName.trim(),
                                            username.trim(),
                                            password.trim(),
                                            selectedRole,
                                            email.trim(),
                                            phone.trim(),
                                            selectedStudentId,
                                            selectedSubjectId,
                                            studentClass.trim(),
                                            controlledClasses.trim(),
                                            taughtSubjects.trim(),
                                            attachedStudents.trim()
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EduIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Créer le compte", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
