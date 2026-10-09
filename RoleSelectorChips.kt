package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.EduIndigo

@Composable
fun RoleSelectorChips(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserRole.values().forEach { role ->
            val isSelected = role == currentRole
            FilterChip(
                selected = isSelected,
                onClick = { onRoleSelected(role) },
                label = {
                    Text(
                        text = role.displayName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    val icon = when (role) {
                        UserRole.DIRECTION -> Icons.Default.AdminPanelSettings
                        UserRole.CAISSE -> Icons.Default.AccountBalanceWallet
                        UserRole.ENSEIGNANT -> Icons.Default.PersonPin
                        UserRole.EDUCATEUR -> Icons.Default.Security
                        UserRole.PARENT -> Icons.Default.FamilyRestroom
                        UserRole.ELEVE -> Icons.Default.School
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(99.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EduIndigo,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                modifier = Modifier.testTag("role_chip_${role.name.lowercase()}")
            )
        }
    }
}

@Composable
fun TabNavigationBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    unreadMessagesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 16.dp,
        divider = {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedTabIndex
            val isMessaging = title == "Messagerie" || title == "Messages"

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isMessaging && unreadMessagesCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text("$unreadMessagesCount")
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_${title.lowercase()}")
            )
        }
    }
}
