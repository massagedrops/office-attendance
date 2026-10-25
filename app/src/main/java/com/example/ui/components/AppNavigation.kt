package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeTopBar(
    officeName: String,
    currentTime: String,
    isArabic: Boolean,
    isAdminUnlocked: Boolean,
    onToggleLanguage: () -> Unit,
    onAdminLockToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = officeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Text(
                                text = "Asia/Riyadh • $currentTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher button
                    AssistChip(
                        onClick = onToggleLanguage,
                        modifier = Modifier
                            .testTag("language_switch_button")
                            .padding(end = 4.dp),
                        label = {
                            Text(
                                text = if (isArabic) "English" else "العربية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryBlue
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Admin Lock / Unlock Status Toggle
                    IconButton(
                        onClick = onAdminLockToggle,
                        modifier = Modifier.testTag("admin_lock_button")
                    ) {
                        if (isAdminUnlocked) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Admin Mode Active",
                                tint = EmeraldSuccess
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Admin Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

data class TabItemData(
    val tab: AppTab,
    val labelKey: String,
    val icon: ImageVector
)

val APP_TABS = listOf(
    TabItemData(AppTab.DASHBOARD, "dashboard", Icons.Default.Dashboard),
    TabItemData(AppTab.QR_SCANNER, "qr_scanner", Icons.Default.QrCodeScanner),
    TabItemData(AppTab.EMPLOYEES, "employees", Icons.Default.Badge),
    TabItemData(AppTab.SHIFTS, "shifts", Icons.Default.Schedule),
    TabItemData(AppTab.ATTENDANCE_RECORDS, "records", Icons.Default.History),
    TabItemData(AppTab.MONTHLY_REPORTS, "reports", Icons.Default.Assessment),
    TabItemData(AppTab.EXCEL_EXPORT, "export", Icons.Default.FileDownload),
    TabItemData(AppTab.SETTINGS, "settings", Icons.Default.Settings)
)

@Composable
fun AppNavigationBar(
    currentTab: AppTab,
    isArabic: Boolean,
    onTabSelected: (AppTab) -> Unit
) {
    val selectedIndex = APP_TABS.indexOfFirst { it.tab == currentTab }.coerceAtLeast(0)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = PrimaryBlue,
                        height = 3.dp
                    )
                }
            },
            divider = {}
        ) {
            APP_TABS.forEachIndexed { index, tabItem ->
                val isSelected = tabItem.tab == currentTab
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tabItem.tab) },
                    modifier = Modifier.testTag("tab_${tabItem.tab.name.lowercase()}"),
                    text = {
                        Text(
                            text = Localization.get(tabItem.labelKey, isArabic),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = tabItem.icon,
                            contentDescription = Localization.get(tabItem.labelKey, isArabic),
                            tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}
