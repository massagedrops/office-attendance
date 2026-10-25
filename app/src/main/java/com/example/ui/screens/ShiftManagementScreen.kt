package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.Shift
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseDanger
import com.example.util.Localization
import com.example.util.TimeUtil

@Composable
fun ShiftManagementScreen(
    shifts: List<Shift>,
    employees: List<Employee>,
    isArabic: Boolean,
    onRequestAdminAuth: (() -> Unit) -> Unit,
    onSaveShift: (Shift) -> Unit,
    onDeleteShift: (Shift) -> Unit,
    onAssignDailyShift: (employeeId: String, date: String, shiftId: String) -> Unit
) {
    var showAddEditShiftDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<Shift?>(null) }
    var showDailyOverrideDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().testTag("shift_management_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Daily Assignment Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.get("shifts", isArabic),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "ورديات مرنة وتخصيص يومي لكل موظف" else "Flexible shifts & daily schedule assignment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onRequestAdminAuth {
                                showDailyOverrideDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("assign_daily_shift_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = Localization.get("assign_daily_shift", isArabic), fontSize = 12.sp)
                    }
                }
            }

            items(shifts) { shift ->
                val isOvernight = isOvernight(shift.startTime, shift.endTime)
                ShiftCard(
                    shift = shift,
                    isOvernight = isOvernight,
                    isArabic = isArabic,
                    onEdit = {
                        onRequestAdminAuth {
                            editingShift = shift
                            showAddEditShiftDialog = true
                        }
                    },
                    onDelete = {
                        onRequestAdminAuth {
                            onDeleteShift(shift)
                        }
                    }
                )
            }
        }

        // Add Shift FAB
        FloatingActionButton(
            onClick = {
                onRequestAdminAuth {
                    editingShift = null
                    showAddEditShiftDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_shift_fab"),
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = Localization.get("add_shift", isArabic))
        }

        if (showAddEditShiftDialog) {
            AddEditShiftDialog(
                shift = editingShift,
                isArabic = isArabic,
                onDismiss = { showAddEditShiftDialog = false },
                onSave = { updatedShift ->
                    onSaveShift(updatedShift)
                    showAddEditShiftDialog = false
                }
            )
        }

        if (showDailyOverrideDialog) {
            DailyShiftOverrideDialog(
                employees = employees,
                shifts = shifts,
                isArabic = isArabic,
                onDismiss = { showDailyOverrideDialog = false },
                onAssign = { empId, date, shiftId ->
                    onAssignDailyShift(empId, date, shiftId)
                    showDailyOverrideDialog = false
                }
            )
        }
    }
}

@Composable
fun ShiftCard(
    shift: Shift,
    isOvernight: Boolean,
    isArabic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shiftName = if (isArabic && shift.nameAr.isNotBlank()) shift.nameAr else shift.nameEn

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("shift_card_${shift.id}")
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isOvernight) Color(0xFF312E81) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOvernight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = if (isOvernight) Color.White else AmberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = shiftName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${shift.startTime} ➔ ${shift.endTime}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryBlue
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOvernight) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Text(
                            text = Localization.get("overnight_notice", isArabic),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF6D28D9),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberBg
                ) {
                    Text(
                        text = "${Localization.get("grace_period_mins", isArabic)}: ${shift.graceMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditShiftDialog(
    shift: Shift?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit
) {
    val isEdit = shift != null
    var id by remember { mutableStateOf(shift?.id ?: "SHIFT_${System.currentTimeMillis() % 1000}") }
    var nameEn by remember { mutableStateOf(shift?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(shift?.nameAr ?: "") }
    var startTime by remember { mutableStateOf(shift?.startTime ?: "14:00") }
    var endTime by remember { mutableStateOf(shift?.endTime ?: "02:00") }
    var graceMinutes by remember { mutableStateOf(shift?.graceMinutes?.toString() ?: "15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_shift_dialog"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isEdit) Localization.get("edit_shift", isArabic) else Localization.get("add_shift", isArabic),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(Localization.get("shift_name_en", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(Localization.get("shift_name_ar", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text(Localization.get("start_time", isArabic)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text(Localization.get("end_time", isArabic)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = graceMinutes,
                    onValueChange = { graceMinutes = it },
                    label = { Text(Localization.get("grace_period_mins", isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOvernight(startTime, endTime)) {
                    Text(
                        text = Localization.get("overnight_notice", isArabic),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6D28D9),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startTime.isNotBlank() && endTime.isNotBlank()) {
                        onSave(
                            Shift(
                                id = id,
                                nameEn = nameEn.ifBlank { "Shift $startTime-$endTime" },
                                nameAr = nameAr.ifBlank { "وردية $startTime-$endTime" },
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                graceMinutes = graceMinutes.toIntOrNull() ?: 15
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyShiftOverrideDialog(
    employees: List<Employee>,
    shifts: List<Shift>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAssign: (employeeId: String, date: String, shiftId: String) -> Unit
) {
    var selectedEmployeeId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
    var selectedShiftId by remember { mutableStateOf(shifts.firstOrNull()?.id ?: "") }
    var selectedDate by remember { mutableStateOf(TimeUtil.todayDateString()) }

    var empDropdownExpanded by remember { mutableStateOf(false) }
    var shiftDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("daily_shift_override_dialog"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = Localization.get("assign_daily_shift", isArabic),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Employee Dropdown
                ExposedDropdownMenuBox(
                    expanded = empDropdownExpanded,
                    onExpandedChange = { empDropdownExpanded = !empDropdownExpanded }
                ) {
                    val currentEmp = employees.firstOrNull { it.id == selectedEmployeeId }
                    val empName = currentEmp?.let {
                        if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.name
                    } ?: selectedEmployeeId

                    OutlinedTextField(
                        value = "$selectedEmployeeId - $empName",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.get("filter_by_employee", isArabic)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = empDropdownExpanded,
                        onDismissRequest = { empDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            val name = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                            DropdownMenuItem(
                                text = { Text("${emp.id} - $name") },
                                onClick = {
                                    selectedEmployeeId = emp.id
                                    empDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date field
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text(Localization.get("select_date", isArabic) + " (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Shift Dropdown
                ExposedDropdownMenuBox(
                    expanded = shiftDropdownExpanded,
                    onExpandedChange = { shiftDropdownExpanded = !shiftDropdownExpanded }
                ) {
                    val currentShift = shifts.firstOrNull { it.id == selectedShiftId }
                    val shiftName = currentShift?.let {
                        if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn
                    } ?: selectedShiftId

                    OutlinedTextField(
                        value = shiftName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.get("assigned_shift", isArabic)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = shiftDropdownExpanded,
                        onDismissRequest = { shiftDropdownExpanded = false }
                    ) {
                        shifts.forEach { s ->
                            val sName = if (isArabic && s.nameAr.isNotBlank()) s.nameAr else s.nameEn
                            DropdownMenuItem(
                                text = { Text(sName) },
                                onClick = {
                                    selectedShiftId = s.id
                                    shiftDropdownExpanded = false
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
                    if (selectedEmployeeId.isNotBlank() && selectedDate.isNotBlank() && selectedShiftId.isNotBlank()) {
                        onAssign(selectedEmployeeId, selectedDate.trim(), selectedShiftId)
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

private fun isOvernight(start: String, end: String): Boolean {
    val sH = start.split(":").firstOrNull()?.toIntOrNull() ?: 0
    val eH = end.split(":").firstOrNull()?.toIntOrNull() ?: 0
    return eH <= sH
}
