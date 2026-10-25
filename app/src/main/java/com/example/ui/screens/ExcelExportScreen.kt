package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.theme.CardBorder
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.util.Localization
import com.example.util.TimeUtil
import java.io.File
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelExportScreen(
    employees: List<Employee>,
    isArabic: Boolean,
    isGeneratingExcel: Boolean,
    lastGeneratedFile: File?,
    generatedFiles: List<File>,
    exportMessage: String?,
    onGenerateIndividual: (employeeId: String, yearMonth: YearMonth) -> Unit,
    onGenerateCombined: (yearMonth: YearMonth) -> Unit,
    onGenerateAllSeparate: (yearMonth: YearMonth) -> Unit,
    onGenerateOnePage: (yearMonth: YearMonth) -> Unit,
    onShareFile: (Context, File, String?) -> Unit,
    onShareFiles: (Context, List<File>, String?) -> Unit
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableStateOf("COMBINED") } // "COMBINED", "INDIVIDUAL", "ALL_SEPARATE" or "ONE_PAGE"
    var selectedEmployeeId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now(TimeUtil.RIYADH_ZONE)) }

    var empDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("excel_export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = Localization.get("export_excel_title", isArabic),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isArabic) "تصدير ملفات Excel بصيغة xlsx ومشاركتها مباشرة عبر واتساب" else "Generate OpenXML Excel workbooks and share to WhatsApp directly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Localization.get("report_format_type", isArabic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Toggle: Combined vs Individual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { selectedReportType = "COMBINED" },
                            modifier = Modifier.weight(1f).testTag("select_combined_report_type"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedReportType == "COMBINED") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedReportType == "COMBINED") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("combined_monthly_report", isArabic), fontSize = 12.sp)
                        }

                        Button(
                            onClick = { selectedReportType = "INDIVIDUAL" },
                            modifier = Modifier.weight(1f).testTag("select_individual_report_type"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedReportType == "INDIVIDUAL") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedReportType == "INDIVIDUAL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("individual_report", isArabic), fontSize = 12.sp)
                        }
                    }

                    // Toggle: separate Excel file for EVERY worker
                    Button(
                        onClick = { selectedReportType = "ALL_SEPARATE" },
                        modifier = Modifier.fillMaxWidth().testTag("select_all_separate_report_type"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedReportType == "ALL_SEPARATE") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedReportType == "ALL_SEPARATE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isArabic) "ملف Excel منفصل لكل موظف" else "Separate Excel for Each Worker",
                            fontSize = 12.sp
                        )
                    }

                    // Toggle: ONE page sheet with all workers (like the paper attendance sheet)
                    Button(
                        onClick = { selectedReportType = "ONE_PAGE" },
                        modifier = Modifier.fillMaxWidth().testTag("select_one_page_report_type"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedReportType == "ONE_PAGE") PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedReportType == "ONE_PAGE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isArabic) "كشف حضور شهري في صفحة واحدة (كل الموظفين)" else "One-Page Sheet (All Workers Together)",
                            fontSize = 12.sp
                        )
                    }

                    // Month Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = PrimaryBlue)
                            Text(
                                text = "${Localization.get("select_month", isArabic)}: ${selectedYearMonth.year} - ${"%02d".format(selectedYearMonth.monthValue)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { selectedYearMonth = selectedYearMonth.minusMonths(1) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("◀")
                            }
                            OutlinedButton(
                                onClick = { selectedYearMonth = selectedYearMonth.plusMonths(1) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("▶")
                            }
                        }
                    }

                    // If Individual, pick employee
                    if (selectedReportType == "INDIVIDUAL") {
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
                                label = { Text(Localization.get("select_employee", isArabic)) },
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
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            when (selectedReportType) {
                                "COMBINED" -> onGenerateCombined(selectedYearMonth)
                                "ALL_SEPARATE" -> onGenerateAllSeparate(selectedYearMonth)
                                "ONE_PAGE" -> onGenerateOnePage(selectedYearMonth)
                                else -> onGenerateIndividual(selectedEmployeeId, selectedYearMonth)
                            }
                        },
                        enabled = !isGeneratingExcel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_excel_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isGeneratingExcel) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("generating_excel", isArabic))
                        } else {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("generate_excel", isArabic), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 9-Column Format & Completion Status Info Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "مواصفات تقرير Excel (أوراق العمل التفصيلية لكل موظف):" else "Excel Report Columns (Detailed Employee Worksheets):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    val colsList = if (isArabic) {
                        listOf(
                            "1. التاريخ",
                            "2. الرقم الوظيفي",
                            "3. اسم الموظف",
                            "4. الوردية المحددة",
                            "5. وقت الحضور",
                            "6. وقت الانصراف",
                            "7. إجمالي ساعات العمل",
                            "8. دقائق التأخير",
                            "9. حالة الحضور",
                            "10. اكتمال الحضور والانصراف (علامة الصح ✓)"
                        )
                    } else {
                        listOf(
                            "1. Date",
                            "2. Employee ID",
                            "3. Employee Name",
                            "4. Assigned Shift",
                            "5. Check-in Time",
                            "6. Check-out Time",
                            "7. Total Working Hours",
                            "8. Late Minutes",
                            "9. Attendance Status",
                            "10. Check-in / Check-out Completion (Tick Mark ✓)"
                        )
                    }
                    colsList.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pair.getOrNull(0) ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pair.getOrNull(1) ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic)
                            "• ورقة العمل الأولى تجمع جميع الموظفين في ملف واحد مع الاسم، التاريخ، وقت الحضور، الانصراف، ساعات العمل، وعلامة الصح (✓).\n• علامة صح خضراء (✓) عند اكتمال الحضور والانصراف.\n• كتابة \"غير مكتمل\" بدون علامة صح عند عدم تسجيل الانصراف.\n• حقول فارغة عند عدم وجود حضور."
                        else
                            "• First sheet displays all employees together in one file with Name, Date, Check-in, Check-out, Duty Hours, and Completion Tick Mark (✓).\n• Green check mark (✓) displayed when both Check-in & Check-out are recorded.\n• Displays \"Incomplete\" (no tick) if Check-out is missing.\n• Left blank if no attendance recorded.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Separate files (one per worker) + share
        if (generatedFiles.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("generated_files_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تم إنشاء ${generatedFiles.size} ملفات (ملف لكل موظف)"
                            else "${generatedFiles.size} separate Excel files created (one per worker)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )

                        Button(
                            onClick = { onShareFiles(context, generatedFiles, null) },
                            modifier = Modifier.fillMaxWidth().testTag("share_all_files_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isArabic) "مشاركة جميع الملفات" else "Share All Files",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        generatedFiles.forEach { f ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = f.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${f.length() / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onShareFile(context, f, "com.whatsapp") },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.White, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onShareFile(context, f, null) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isArabic) "مشاركة" else "Share", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Generated File Card & WhatsApp Share
        lastGeneratedFile?.let { file ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("generated_file_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = Localization.get("report_generated_success", isArabic),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                )
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${file.length() / 1024} KB • OpenXML Workbook (.xlsx)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        // WhatsApp and System Share Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onShareFile(context, file, "com.whatsapp")
                                },
                                modifier = Modifier.weight(1f).testTag("share_whatsapp_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.get("share_via_whatsapp", isArabic), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onShareFile(context, file, null)
                                },
                                modifier = Modifier.weight(1f).testTag("share_all_apps_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.get("share_report", isArabic), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
