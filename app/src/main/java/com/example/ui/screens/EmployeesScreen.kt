package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.Shift
import com.example.ui.theme.CardBorder
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseBg
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextMuted
import com.example.util.Localization
import com.example.util.QrCodeGenerator
import com.example.util.TimeUtil
import com.example.util.IdCardGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext

@Composable
fun EmployeesScreen(
    employees: List<Employee>,
    shifts: List<Shift>,
    records: List<AttendanceRecord> = emptyList(),
    isArabic: Boolean,
    isAdminUnlocked: Boolean,
    onRequestAdminAuth: (() -> Unit) -> Unit,
    onSaveEmployee: (Employee) -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onRegenerateQr: (String) -> Unit
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedEmployeeForQr by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    val activeRecordsByEmployee = remember(records) {
        val today = TimeUtil.todayDateString()
        records.filter { it.attendanceDate == today && it.checkOutTime == null }
            .associateBy { it.employeeId }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("employees_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${Localization.get("employees", isArabic)} (${employees.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "إدارة الموظفين والرموز التعريفية والورديات" else "Manage employee profiles, QR codes, and shifts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                val ctx = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val list = employees.filter { it.isActive }
                        if (list.isNotEmpty()) {
                            val f = IdCardGenerator.saveAllCardsPdf(ctx, list)
                            IdCardGenerator.shareFile(ctx, f, "application/pdf")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "تصدير كل بطاقات الهوية (PDF)" else "Export all ID cards (PDF)")
                }
            }

            items(employees) { employee ->
                EmployeeCard(
                    employee = employee,
                    shifts = shifts,
                    activeRecord = activeRecordsByEmployee[employee.id],
                    isArabic = isArabic,
                    onViewQr = { selectedEmployeeForQr = employee },
                    onEdit = {
                        onRequestAdminAuth {
                            editingEmployee = employee
                            showAddEditDialog = true
                        }
                    },
                    onDelete = {
                        onRequestAdminAuth {
                            employeeToDelete = employee
                        }
                    },
                    onToggleActive = { newActive ->
                        onRequestAdminAuth {
                            onSaveEmployee(employee.copy(isActive = newActive))
                        }
                    }
                )
            }
        }

        // Add Employee FAB
        FloatingActionButton(
            onClick = {
                onRequestAdminAuth {
                    editingEmployee = null
                    showAddEditDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_employee_fab"),
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = Localization.get("add_employee", isArabic))
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditEmployeeDialog(
                employee = editingEmployee,
                shifts = shifts,
                isArabic = isArabic,
                onDismiss = { showAddEditDialog = false },
                onSave = { updatedEmp ->
                    onSaveEmployee(updatedEmp)
                    showAddEditDialog = false
                }
            )
        }

        // QR Code Viewer & Regenerator Dialog
        selectedEmployeeForQr?.let { emp ->
            EmployeeQrDialog(
                employee = emp,
                isArabic = isArabic,
                isAdminUnlocked = isAdminUnlocked,
                onRequestAdminAuth = onRequestAdminAuth,
                onDismiss = { selectedEmployeeForQr = null },
                onRegenerate = {
                    onRegenerateQr(emp.id)
                    // Refresh current dialog view
                    selectedEmployeeForQr = emp.copy(qrCode = "EMP-${emp.id}-${System.currentTimeMillis() % 10000}")
                }
            )
        }

        // Delete confirmation dialog
        employeeToDelete?.let { emp ->
            AlertDialog(
                onDismissRequest = { employeeToDelete = null },
                title = { Text(Localization.get("delete_employee", isArabic)) },
                text = { Text(Localization.get("delete_confirm", isArabic)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteEmployee(emp)
                            employeeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Text(Localization.get("confirm", isArabic))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { employeeToDelete = null }) {
                        Text(Localization.get("cancel", isArabic))
                    }
                }
            )
        }
    }
}

@Composable
fun EmployeeCard(
    employee: Employee,
    shifts: List<Shift>,
    activeRecord: AttendanceRecord? = null,
    isArabic: Boolean,
    onViewQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val assignedShift = shifts.firstOrNull { it.id == employee.assignedShiftId }
    val displayName = if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name
    val isCheckedInNow = activeRecord != null

    var currentTickerMillis by remember(activeRecord?.id) {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(activeRecord?.id) {
        if (isCheckedInNow) {
            while (true) {
                currentTickerMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("employee_card_${employee.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCheckedInNow) EmeraldBg
                                else if (employee.isActive) PrimaryBlue.copy(alpha = 0.12f)
                                else TextMuted.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.id.takeLast(3),
                            fontWeight = FontWeight.Bold,
                            color = if (isCheckedInNow) EmeraldSuccess else if (employee.isActive) PrimaryBlue else TextMuted,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${employee.id} • ${employee.phone.ifBlank { "No phone" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = employee.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldSuccess, checkedTrackColor = EmeraldBg)
                )
            }

            // Live Working Timer on Employee Card if currently checked in
            if (activeRecord != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "تسجيل الحضور: ${activeRecord.checkInFormatted}" else "Checked in: ${activeRecord.checkInFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = TimeUtil.formatLiveWorkingDuration(activeRecord.checkInTime, currentTickerMillis, isArabic),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shift Badge and QR Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = assignedShift?.nameEn ?: employee.assignedShiftId,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onViewQr,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("view_qr_${employee.id}")
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeDialog(
    employee: Employee?,
    shifts: List<Shift>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    val isEdit = employee != null
    var id by remember { mutableStateOf(employee?.id ?: "EMP00${(9..99).random()}") }
    var nameEn by remember { mutableStateOf(employee?.name ?: "") }
    var nameAr by remember { mutableStateOf(employee?.nameAr ?: "") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var designation by remember { mutableStateOf(employee?.designation ?: "") }
    var photoUri by remember { mutableStateOf(employee?.photoUri) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            IdCardGenerator.importPhoto(context, uri, id.trim().ifBlank { "emp" })?.let { photoUri = it }
        }
    }
    var assignedShiftId by remember { mutableStateOf(employee?.assignedShiftId ?: (shifts.firstOrNull()?.id ?: "SHIFT_A")) }
    var isActive by remember { mutableStateOf(employee?.isActive ?: true) }

    var expandedShiftDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_employee_dialog"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isEdit) Localization.get("edit_employee", isArabic) else Localization.get("add_employee", isArabic),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (photoUri != null) (if (isArabic) "تغيير الصورة ✓" else "Change photo ✓")
                        else (if (isArabic) "إضافة صورة" else "Add photo")
                    )
                }

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text(if (isArabic) "المسمى الوظيفي" else "Designation (e.g. Receptionist)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = id,
                    onValueChange = { if (!isEdit) id = it },
                    label = { Text(Localization.get("employee_id", isArabic)) },
                    singleLine = true,
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(Localization.get("full_name_en", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(Localization.get("full_name_ar", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Localization.get("phone_number", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Shift Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedShiftDropdown,
                    onExpandedChange = { expandedShiftDropdown = !expandedShiftDropdown }
                ) {
                    val currentShiftName = shifts.firstOrNull { it.id == assignedShiftId }?.let {
                        if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn
                    } ?: assignedShiftId

                    OutlinedTextField(
                        value = currentShiftName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.get("assigned_shift", isArabic)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedShiftDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedShiftDropdown,
                        onDismissRequest = { expandedShiftDropdown = false }
                    ) {
                        shifts.forEach { shift ->
                            val sName = if (isArabic && shift.nameAr.isNotBlank()) shift.nameAr else shift.nameEn
                            DropdownMenuItem(
                                text = { Text(sName) },
                                onClick = {
                                    assignedShiftId = shift.id
                                    expandedShiftDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameEn.isNotBlank() || nameAr.isNotBlank()) {
                        val finalQr = employee?.qrCode ?: id
                        onSave(
                            Employee(
                                id = id.trim(),
                                name = nameEn.ifBlank { nameAr },
                                nameAr = nameAr.ifBlank { nameEn },
                                phone = phone.trim(),
                                photoUri = photoUri,
                                designation = designation.trim(),
                                qrCode = finalQr,
                                assignedShiftId = assignedShiftId,
                                isActive = isActive
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Localization.get("save", isArabic))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.get("cancel", isArabic))
            }
        }
    )
}

@Composable
fun EmployeeQrDialog(
    employee: Employee,
    isArabic: Boolean,
    isAdminUnlocked: Boolean,
    onRequestAdminAuth: (() -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    val cardBitmap: Bitmap? = remember(employee) {
        try { IdCardGenerator.generate(context, employee) } catch (_: Exception) { null }
    }

    val displayName = if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("employee_qr_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "${Localization.get("qr_code_for", isArabic)} $displayName",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${Localization.get("employee_id", isArabic)}: ${employee.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cardBitmap != null) {
                    Image(
                        bitmap = cardBitmap.asImageBitmap(),
                        contentDescription = "Employee ID Card",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Button(
                        onClick = {
                            val f = IdCardGenerator.saveCardPng(context, employee)
                            IdCardGenerator.shareFile(context, f, "image/png")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "مشاركة / حفظ البطاقة" else "Share / Save ID card")
                    }
                } else {
                    Text("Unable to generate ID card")
                }

                Text(
                    text = "QR String: ${employee.qrCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = Localization.get("print_save_qr", isArabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Localization.get("close", isArabic))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onRequestAdminAuth {
                        onRegenerate()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Localization.get("regenerate_qr", isArabic))
            }
        }
    )
}
