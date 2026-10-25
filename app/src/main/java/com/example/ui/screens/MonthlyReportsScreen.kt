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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.data.model.Shift
import com.example.ui.AppTab
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseBg
import com.example.ui.theme.RoseDanger
import com.example.util.Localization
import com.example.util.TimeUtil
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    employees: List<Employee>,
    shifts: List<Shift>,
    allRecords: List<AttendanceRecord>,
    isArabic: Boolean,
    onNavigateToTab: (AppTab) -> Unit,
    onExportIndividual: (employeeId: String, yearMonth: YearMonth) -> Unit,
    onExportCombined: (yearMonth: YearMonth) -> Unit
) {
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now(TimeUtil.RIYADH_ZONE)) }
    var selectedEmployeeId by remember { mutableStateOf<String?>(null) } // null = Combined all employees

    val shiftMap = remember(shifts) { shifts.associateBy { it.id } }

    val monthPrefix = remember(selectedYearMonth) {
        "${selectedYearMonth.year}-${"%02d".format(selectedYearMonth.monthValue)}"
    }

    val monthRecords = remember(allRecords, monthPrefix) {
        allRecords.filter { it.attendanceDate.startsWith(monthPrefix) }
    }

    val daysInMonth = selectedYearMonth.lengthOfMonth()
    val scheduledWorkDays = (1..daysInMonth).count { day ->
        val date = selectedYearMonth.atDay(day)
        date.dayOfWeek.value != 5 && date.dayOfWeek.value != 6 // Sun - Thu
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = Localization.get("monthly_reports", isArabic),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isArabic) "تقارير الحضور والغياب وساعات العمل الشهرية الشاملة" else "Monthly attendance, absent days, and working hours analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Month Selector & Export Action Row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = PrimaryBlue)
                            Text(
                                text = "${Localization.get("select_month", isArabic)}: ${selectedYearMonth.year} - ${"%02d".format(selectedYearMonth.monthValue)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { selectedYearMonth = selectedYearMonth.minusMonths(1) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("◀")
                            }
                            OutlinedButton(
                                onClick = { selectedYearMonth = selectedYearMonth.plusMonths(1) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("▶")
                            }
                        }
                    }

                    // Export Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedEmployeeId == null) {
                                    onExportCombined(selectedYearMonth)
                                } else {
                                    onExportIndividual(selectedEmployeeId!!, selectedYearMonth)
                                }
                                onNavigateToTab(AppTab.EXCEL_EXPORT)
                            },
                            modifier = Modifier.weight(1f).testTag("export_excel_report_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("export_excel", isArabic), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Employee Selection Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedEmployeeId == null,
                        onClick = { selectedEmployeeId = null },
                        label = { Text(Localization.get("all_employees", isArabic)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(employees) { emp ->
                    val isSelected = selectedEmployeeId == emp.id
                    val displayName = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEmployeeId = emp.id },
                        label = { Text("${emp.id} - ${displayName.split(" ").firstOrNull() ?: displayName}") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Summary View: Combined or Individual
        if (selectedEmployeeId == null) {
            // Combined Table of all employees
            items(employees) { emp ->
                val empRecords = monthRecords.filter { it.employeeId == emp.id }
                val daysWorked = empRecords.map { it.attendanceDate }.distinct().size
                val completedCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime != null }
                val incompleteCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime == null }
                val totalHours = empRecords.sumOf { it.totalHours }
                val totalLate = empRecords.sumOf { it.lateMinutes }
                val absentDays = maxOf(0, scheduledWorkDays - daysWorked)
                val shift = shiftMap[emp.assignedShiftId]

                EmployeeMonthSummaryCard(
                    employee = emp,
                    shift = shift,
                    daysWorked = daysWorked,
                    completedDays = completedCount,
                    incompleteRecords = incompleteCount,
                    totalHours = totalHours,
                    totalLateMinutes = totalLate,
                    absentDays = absentDays,
                    isArabic = isArabic,
                    onViewIndividual = { selectedEmployeeId = emp.id }
                )
            }
        } else {
            // Individual Detailed Month Report
            val targetEmp = employees.firstOrNull { it.id == selectedEmployeeId }
            if (targetEmp != null) {
                val empRecords = monthRecords.filter { it.employeeId == targetEmp.id }.sortedBy { it.attendanceDate }
                val daysWorked = empRecords.map { it.attendanceDate }.distinct().size
                val completedCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime != null }
                val incompleteCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime == null }
                val totalHours = empRecords.sumOf { it.totalHours }
                val totalLate = empRecords.sumOf { it.lateMinutes }
                val absentDays = maxOf(0, scheduledWorkDays - daysWorked)
                val shift = shiftMap[targetEmp.assignedShiftId]

                item {
                    IndividualMonthHeaderCard(
                        employee = targetEmp,
                        shift = shift,
                        daysWorked = daysWorked,
                        completedDays = completedCount,
                        incompleteRecords = incompleteCount,
                        totalHours = totalHours,
                        totalLateMinutes = totalLate,
                        absentDays = absentDays,
                        isArabic = isArabic
                    )
                }

                item {
                    Text(
                        text = Localization.get("daily_breakdown", isArabic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (empRecords.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isArabic) "لا توجد سجلات حضور لهذا الشهر" else "No attendance recorded this month",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(empRecords) { rec ->
                        DailyRecordRowCard(record = rec, isArabic = isArabic)
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeMonthSummaryCard(
    employee: Employee,
    shift: Shift?,
    daysWorked: Int,
    completedDays: Int,
    incompleteRecords: Int,
    totalHours: Double,
    totalLateMinutes: Int,
    absentDays: Int,
    isArabic: Boolean,
    onViewIndividual: () -> Unit
) {
    val displayName = if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = employee.id.takeLast(3), fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 12.sp)
                    }
                    Column {
                        Text(text = displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "${employee.id} • ${shift?.nameEn ?: employee.assignedShiftId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onViewIndividual,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(text = Localization.get("view_details", isArabic), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats in row: Completed Days (✓), Incomplete, Total Hours, Late Mins, Absent Days
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isArabic) "مكتمل ✓" else "Completed ✓", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    Text(text = "$completedDays", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                }
                if (incompleteRecords > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isArabic) "غير مكتمل" else "Incomplete", style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Bold)
                        Text(text = "$incompleteRecords", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("days_worked", isArabic), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$daysWorked", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("total_hours", isArabic), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = TimeUtil.formatHoursToDurationString(totalHours, isArabic), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("total_absent_days", isArabic), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$absentDays", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (absentDays > 0) RoseDanger else EmeraldSuccess)
                }
            }
        }
    }
}

@Composable
fun IndividualMonthHeaderCard(
    employee: Employee,
    shift: Shift?,
    daysWorked: Int,
    completedDays: Int,
    incompleteRecords: Int,
    totalHours: Double,
    totalLateMinutes: Int,
    absentDays: Int,
    isArabic: Boolean
) {
    val displayName = if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = displayName, style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
            Text(text = "${employee.id} • ${shift?.nameEn ?: employee.assignedShiftId}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f)))

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isArabic) "مكتمل ✓" else "Completed ✓", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                    Text(text = "$completedDays", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
                if (incompleteRecords > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isArabic) "غير مكتمل" else "Incomplete", fontSize = 11.sp, color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold)
                        Text(text = "$incompleteRecords", fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A), fontSize = 16.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("days_worked", isArabic), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "$daysWorked", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("total_hours", isArabic), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = TimeUtil.formatHoursToDurationString(totalHours, isArabic), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = Localization.get("total_absent_days", isArabic), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "$absentDays", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DailyRecordRowCard(record: AttendanceRecord, isArabic: Boolean) {
    val isOngoing = record.checkOutTime == null
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = record.attendanceDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!isOngoing) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldBg
                        ) {
                            Text(
                                text = "✓ ${if (isArabic) "مكتمل" else "Completed"}",
                                color = EmeraldSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (isArabic) "غير مكتمل" else "Incomplete",
                                color = AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.checkInFormatted} ➔ ${record.checkOutFormatted ?: (if (isArabic) "قيد العمل" else "In Progress")}",
                    fontSize = 12.sp,
                    color = if (isOngoing) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val durationDisplay = if (isOngoing) {
                    TimeUtil.formatLiveWorkingDuration(record.checkInTime, currentTickerMillis, isArabic)
                } else {
                    TimeUtil.formatCompletedWorkingDuration(record.totalHours, isArabic)
                }
                Text(
                    text = durationDisplay,
                    fontWeight = FontWeight.Bold,
                    color = if (isOngoing) EmeraldSuccess else PrimaryBlue,
                    fontSize = 13.sp
                )
                if (record.lateMinutes > 0) {
                    Text(text = "${Localization.get("late", isArabic)}: ${record.lateMinutes}m", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
