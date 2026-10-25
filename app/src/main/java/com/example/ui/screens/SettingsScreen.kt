package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.util.Localization
import com.example.util.WhatsAppUtil

@Composable
fun SettingsScreen(
    settings: AppSettings?,
    isArabic: Boolean,
    onToggleLanguage: () -> Unit,
    onRequestAdminAuth: (() -> Unit) -> Unit,
    onSaveSettings: (officeName: String, adminPin: String, gracePeriod: Int, bossWhatsAppNumber: String) -> Unit,
    onSaveBossWhatsApp: (bossWhatsAppNumber: String) -> Unit = {}
) {
    val context = LocalContext.current
    var officeName by remember { mutableStateOf(settings?.officeName ?: "Office Attendance Manager") }
    var adminPin by remember { mutableStateOf(settings?.adminPin ?: "1234") }
    var gracePeriodStr by remember { mutableStateOf(settings?.lateGraceMinutes?.toString() ?: "15") }
    var bossWhatsAppNumber by remember { mutableStateOf(settings?.bossWhatsAppNumber ?: "") }
    var saveSuccessMessage by remember { mutableStateOf(false) }
    var saveWhatsAppSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        if (settings != null) {
            officeName = settings.officeName
            adminPin = settings.adminPin
            gracePeriodStr = settings.lateGraceMinutes.toString()
            bossWhatsAppNumber = settings.bossWhatsAppNumber
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = Localization.get("settings", isArabic),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isArabic) "إعدادات المكتب وكلمة سر الإدارة والمنطقة الزمنية" else "Office configuration, admin credentials, and timezone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language Switch Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = PrimaryBlue)
                        }
                        Column {
                            Text(
                                text = Localization.get("language", isArabic),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isArabic) "العربية (RTL) مفعلة" else "English (LTR) Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (isArabic) "Switch to English" else "التبديل إلى العربية")
                    }
                }
            }
        }

        // Office Settings Card (Requires Admin)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Localization.get("office_settings", isArabic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = officeName,
                        onValueChange = { officeName = it },
                        label = { Text(Localization.get("office_name", isArabic)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { if (it.length <= 8) adminPin = it },
                        label = { Text(Localization.get("admin_pin", isArabic)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = gracePeriodStr,
                        onValueChange = { gracePeriodStr = it },
                        label = { Text(Localization.get("grace_period_mins", isArabic)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Timer, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onRequestAdminAuth {
                                onSaveSettings(
                                    officeName.trim(),
                                    adminPin.trim(),
                                    gracePeriodStr.toIntOrNull() ?: 15,
                                    bossWhatsAppNumber.trim()
                                )
                                saveSuccessMessage = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.get("save", isArabic), fontWeight = FontWeight.Bold)
                    }

                    if (saveSuccessMessage) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isArabic) "تم حفظ الإعدادات بنجاح" else "Settings saved successfully",
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // Boss WhatsApp Settings Section (Directly below Office Settings)
        // -----------------------------------------------------------------
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("boss_whatsapp_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with WhatsApp Green Icon and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = Localization.get("boss_whatsapp_settings", isArabic),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isArabic) "رمز الدولة للمملكة العربية السعودية (+966)" else "Saudi Arabia Country Code (+966)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = Localization.get("boss_whatsapp_desc", isArabic),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    // Current Saved Status Banner
                    val isNumberSaved = settings?.bossWhatsAppNumber?.isNotBlank() == true
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNumberSaved) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isNumberSaved) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isNumberSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isNumberSaved) {
                                    "${Localization.get("boss_number_saved_hint", isArabic)}: ${settings?.bossWhatsAppNumber}"
                                } else {
                                    "${Localization.get("boss_number_saved_hint", isArabic)}: ${Localization.get("not_configured", isArabic)}"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isNumberSaved) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isNumberSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Boss WhatsApp Number Input with Saudi country code (+966)
                    OutlinedTextField(
                        value = bossWhatsAppNumber,
                        onValueChange = {
                            bossWhatsAppNumber = it
                            saveWhatsAppSuccessMessage = false
                        },
                        label = { Text(Localization.get("boss_whatsapp_number", isArabic)) },
                        placeholder = { Text("+966 50 123 4567") },
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🇸🇦 +966",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                text = Localization.get("boss_whatsapp_help", isArabic),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("boss_whatsapp_input")
                    )

                    // Quick prefix helper buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "بادئة سريعة:" else "Quick prefix:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                if (!bossWhatsAppNumber.startsWith("+966")) {
                                    bossWhatsAppNumber = "+9665"
                                }
                            }
                        ) {
                            Text(
                                text = "+966 5...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Save and Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleanNum = bossWhatsAppNumber.trim()
                                onSaveBossWhatsApp(cleanNum)
                                // Also sync in general settings
                                onSaveSettings(
                                    officeName.trim(),
                                    adminPin.trim(),
                                    gracePeriodStr.toIntOrNull() ?: 15,
                                    cleanNum
                                )
                                saveWhatsAppSuccessMessage = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_boss_whatsapp_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.get("save_boss_whatsapp", isArabic),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (bossWhatsAppNumber.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val testMsg = if (isArabic) {
                                        "مرحباً، هذه رسالة تجريبية لتأكيد رقم واتساب المدير المسجل في نظام الحضور والانصراف."
                                    } else {
                                        "Hello, this is a test message to confirm the Boss WhatsApp number registered in the Office Attendance system."
                                    }
                                    WhatsAppUtil.openWhatsApp(context, bossWhatsAppNumber, testMsg)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("test_whatsapp_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.get("test_whatsapp", isArabic),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Success Feedback Banner
                    if (saveWhatsAppSuccessMessage) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("boss_whatsapp_save_success")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = Localization.get("boss_whatsapp_saved", isArabic),
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timezone & Database Info Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Localization.get("system_info", isArabic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Text(text = Localization.get("timezone", isArabic), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(text = "Asia/Riyadh (UTC+3)", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Text(text = "Database Engine", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(text = "Room SQLite (Local Persistent)", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Text(text = "Version", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(text = "1.0.0 (Production)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
