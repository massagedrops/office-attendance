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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardBorder
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseBg
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextMuted
import com.example.util.Localization
import com.example.util.TimeUtil

@Composable
fun AttendanceRecordsScreen(
    records: List<AttendanceRecord>,
    employees: List<Employee>,
    isArabic: Boolean,
    onRequestAdminAuth: (() -> Unit) -> Unit,
    onSaveCorrection: (AttendanceRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) } // null = All
    var selectedEmployeeFilter by remember { mutableStateOf<String?>(null) } // null = All

    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<Long?>(null) }

    val filteredRecords = remember(records, searchQuery, selectedStatusFilter, selectedEmployeeFilter) {
        records.filter { rec ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                rec.employeeName.contains(searchQuery, ignoreCase = true) ||
                        rec.employeeId.contains(searchQuery, ignoreCase = true) ||
                        rec.attendanceDate.contains(searchQuery, ignoreCase = true)
            }
            val matchesStatus = when (selectedStatusFilter) {
                null -> true
                "CHECKED_IN" -> rec.checkOutTime == null
                "COMPLETED" -> rec.checkOutTime != null
                "LATE" -> rec.lateMinutes > 0
                else -> rec.status == selectedStatusFilter
            }
            val matchesEmp = if (selectedEmployeeFilter == null) true else rec.employeeId == selectedEmployeeFilter
            matchesSearch && matchesStatus && matchesEmp
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("attendance_records_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            item {
                Column {
                    Text(
                        text = "${Localization.get("records", isArabic)} (${filteredRecords.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isArabic) "سجل الحضور الكامل والبحث والتعديل الإداري" else "Detailed attendance history and admin correction log",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("records_search_input"),
                    placeholder = { Text(Localization.get("search_hint", isArabic)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        Pair(null, Localization.get("filter_by_status", isArabic)),
                        Pair("CHECKED_IN", Localization.get("checked_in_now", isArabic)),
                        Pair("COMPLETED", Localization.get("completed", isArabic)),
                        Pair("LATE", Localization.get("late", isArabic))
                    )
                    items(filters) { (key, label) ->
                        val isSelected = selectedStatusFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatusFilter = if (isSelected) null else key },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Records List
            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد سجلات تطابق البحث" else "No attendance records found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRecords) { record ->
                    AttendanceRecordCard(
                        record = record,
                        isArabic = isArabic,
                        onEdit = {
                            onRequestAdminAuth {
                                editingRecord = record
                            }
                        },
                        onDelete = {
                            onRequestAdminAuth {
                                recordToDelete = record.id
                            }
                        }
                    )
                }
            }
        }

        // Edit / Correction Dialog
        editingRecord?.let { record ->
            AdminRecordCorrectionDialog(
                record = record,
                isArabic = isArabic,
                onDismiss = { editingRecord = null },
                onSave = { updated ->
                    onSaveCorrection(updated)
                    editingRecord = null
                }
            )
        }

        // Delete Confirm
        recordToDelete?.let { id ->
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text(Localization.get("delete_confirm", isArabic)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteRecord(id)
                            recordToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Text(Localization.get("confirm", isArabic))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text(Localization.get("cancel", isArabic))
                    }
                }
            )
        }
    }
}

@Composable
fun AttendanceRecordCard(
    record: AttendanceRecord,
    isArabic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOngoing = record.checkOutTime == null

    // Real-time ticking state that updates every second for active check-ins
    var currentTickerMillis by remember(record.id, isOngoing) {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(record.id, isOngoing) {
        if (isOngoing) {
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
        modifier = Modifier.fillMaxWidth().testTag("attendance_record_${record.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, ID, and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.employeeName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${record.employeeId} • ${record.assignedShiftName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = record.attendanceDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Times Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = Localization.get("check_in", isArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = record.checkInFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                }

                Column {
                    Text(
                        text = Localization.get("check_out", isArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = record.checkOutFormatted ?: (if (isArabic) "قيد العمل" else "In Progress"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOngoing) AmberWarning else PrimaryBlue
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.get("working_hours", isArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    val durationDisplay = if (isOngoing) {
                        TimeUtil.formatLiveWorkingDuration(record.checkInTime, currentTickerMillis, isArabic)
                    } else {
                        TimeUtil.formatCompletedWorkingDuration(record.totalHours, isArabic)
                    }
                    Text(
                        text = durationDisplay,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOngoing) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Badges & Admin notes
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isOngoing) {
                        Surface(shape = RoundedCornerShape(6.dp), color = EmeraldBg) {
                            Text(
                                text = "• " + (if (isArabic) "قيد العمل الآن" else "Active Now"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (record.lateMinutes > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = AmberBg) {
                            Text(
                                text = "${Localization.get("late", isArabic)}: ${record.lateMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(6.dp), color = EmeraldBg) {
                            Text(
                                text = Localization.get("ontime", isArabic),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (record.isManualOverride) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE0E7FF)) {
                            Text(
                                text = if (isArabic) "معدل إدارياً" else "Admin Override",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Record", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Record", tint = RoseDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (!record.adminNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${Localization.get("correction_notes", isArabic)}: ${record.adminNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdminRecordCorrectionDialog(
    record: AttendanceRecord,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (AttendanceRecord) -> Unit
) {
    var checkInFormatted by remember { mutableStateOf(record.checkInFormatted) }
    var checkOutFormatted by remember {
        mutableStateOf(
            record.checkOutFormatted ?: TimeUtil.currentTimeString()
        )
    }
    var totalHoursStr by remember {
        val initialHours = if (record.totalHours > 0.0) {
            record.totalHours
        } else {
            val inM = record.checkInTime
            val outM = record.checkOutTime ?: System.currentTimeMillis()
            TimeUtil.calculateWorkingHours(inM, outM)
        }
        mutableStateOf("%.2f".format(initialHours))
    }
    var lateMinutesStr by remember { mutableStateOf(record.lateMinutes.toString()) }
    var adminNotes by remember { mutableStateOf(record.adminNotes ?: "") }

    // Helper to recalculate duration when check-in or check-out times change
    fun tryRecalculateHours(inTime: String, outTime: String) {
        val inMillis = TimeUtil.parseDateTimeToMillis(record.attendanceDate, inTime)
            ?: record.checkInTime
        val outMillis = TimeUtil.parseDateTimeToMillis(record.attendanceDate, outTime, inMillis)
        if (outMillis != null && inMillis > 0L) {
            val calc = TimeUtil.calculateWorkingHours(inMillis, outMillis)
            totalHoursStr = "%.2f".format(calc)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_correction_dialog"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "${Localization.get("admin_correction", isArabic)} - ${record.employeeName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = checkInFormatted,
                    onValueChange = {
                        checkInFormatted = it
                        tryRecalculateHours(it, checkOutFormatted)
                    },
                    label = { Text(Localization.get("check_in", isArabic) + " (HH:mm:ss)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = checkOutFormatted,
                    onValueChange = {
                        checkOutFormatted = it
                        tryRecalculateHours(checkInFormatted, it)
                    },
                    label = { Text(Localization.get("check_out", isArabic) + " (HH:mm:ss)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalHoursStr,
                        onValueChange = { totalHoursStr = it },
                        label = { Text(Localization.get("working_hours", isArabic)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lateMinutesStr,
                        onValueChange = { lateMinutesStr = it },
                        label = { Text(Localization.get("late", isArabic) + " (m)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = { Text(Localization.get("correction_notes", isArabic)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedCheckInTime = TimeUtil.parseDateTimeToMillis(
                        record.attendanceDate,
                        checkInFormatted
                    ) ?: record.checkInTime

                    val resolvedCheckOutTime = TimeUtil.parseDateTimeToMillis(
                        record.attendanceDate,
                        checkOutFormatted,
                        resolvedCheckInTime
                    ) ?: (record.checkOutTime ?: System.currentTimeMillis())

                    val calculatedHours = if (totalHoursStr.toDoubleOrNull() != null) {
                        totalHoursStr.toDouble()
                    } else {
                        TimeUtil.calculateWorkingHours(resolvedCheckInTime, resolvedCheckOutTime)
                    }

                    val updated = record.copy(
                        checkInTime = resolvedCheckInTime,
                        checkInFormatted = checkInFormatted.trim(),
                        checkOutTime = resolvedCheckOutTime,
                        checkOutFormatted = checkOutFormatted.trim(),
                        totalHours = calculatedHours,
                        lateMinutes = lateMinutesStr.toIntOrNull() ?: record.lateMinutes,
                        adminNotes = adminNotes.trim(),
                        isManualOverride = true,
                        status = if ((lateMinutesStr.toIntOrNull() ?: record.lateMinutes) > 0) "LATE_COMPLETED" else "COMPLETED"
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Localization.get("save_correction", isArabic))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.get("cancel", isArabic))
            }
        }
    )
}
