package com.example.util

import android.content.Context
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.Shift
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {

    const val STYLE_NORMAL = 0
    const val STYLE_HEADER = 1
    const val STYLE_COMPLETED_TICK = 2   // Bold green text (#047857), soft green fill (#ECFDF5), centered
    const val STYLE_INCOMPLETE = 3       // Bold amber text (#B45309), soft amber fill (#FEF3C7), centered
    const val STYLE_TITLE = 4            // Bold navy text (#1E3A8A) 14pt
    const val STYLE_MUTED = 5            // Muted gray text (#9CA3AF), centered
    const val STYLE_SUMMARY_LABEL = 6    // Bold dark text, soft gray fill (#F3F4F6)
    const val STYLE_CENTERED = 7         // Dark text, centered
    const val STYLE_SUBTITLE = 8         // Italic muted text
    const val STYLE_NAME_HEADER = 10     // Bold dark text, gray fill, centered, shrink-to-fit (worker name header)
    const val STYLE_TITLE_CENTERED = 11  // Bold navy 14pt, centered, no border
    const val STYLE_KPI_VALUE = 9        // Bold blue text (#1D4ED8) 12pt, soft blue fill (#EFF6FF), centered

    data class StyledCell(
        val text: String,
        val style: Int = STYLE_NORMAL
    )

    data class SheetColumnDef(
        val width: Double
    )

    data class SheetData(
        val title: String,
        val columns: List<SheetColumnDef>,
        val rows: List<List<StyledCell>>,
        val freezeRow: Int? = null,
        val merges: List<String> = emptyList(),
        val landscapeOnePage: Boolean = false
    )

    data class EmployeeReportSummary(
        val employee: Employee,
        val assignedShift: Shift?,
        val records: List<AttendanceRecord>,
        val totalDaysWorked: Int,
        val completedDays: Int,
        val incompleteRecords: Int,
        val totalWorkingHours: Double,
        val totalLateMinutes: Int,
        val absentDays: Int
    )

    /**
     * Exports a dedicated individual employee monthly report with the 9 mandatory columns:
     * 1. Date
     * 2. Employee ID
     * 3. Employee Name
     * 4. Assigned Shift
     * 5. Check-in Time
     * 6. Check-out Time
     * 7. Total Working Hours
     * 8. Attendance Status
     * 9. Check-in / Check-out Completion (Tick Mark)
     */
    fun exportIndividualReport(
        context: Context,
        officeName: String,
        employee: Employee,
        shift: Shift?,
        records: List<AttendanceRecord>,
        yearMonth: YearMonth,
        isArabic: Boolean,
        includeNameInFileName: Boolean = false
    ): File {
        val nameSlug = if (includeNameInFileName) {
            val base = employee.name.trim()
                .replace("[^\\p{L}\\p{N}]+".toRegex(), "_")
                .trim('_')
                .take(30)
            if (base.isNotBlank()) "_$base" else ""
        } else ""
        val fileName = "Attendance_${employee.id}${nameSlug}_${yearMonth.year}_${"%02d".format(yearMonth.monthValue)}.xlsx"
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsDir, fileName)

        val cleanTitle = (if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name)
            .take(25)
            .replace("[:\\\\/?*\\[\\]]".toRegex(), "")

        val sheetData = buildEmployeeSheetData(
            sheetTitle = if (cleanTitle.isBlank()) employee.id else cleanTitle,
            officeName = officeName,
            employee = employee,
            shift = shift,
            records = records,
            yearMonth = yearMonth,
            isArabic = isArabic
        )

        writeMultiSheetXlsx(outputFile, listOf(sheetData), isArabic)
        return outputFile
    }

    /**
     * Exports a combined monthly report containing:
     * 1. Summary worksheet with all employees, total completed days, and incomplete records.
     * 2. Dedicated worksheet for EACH employee with the exact 9 columns in order.
     */
    fun exportCombinedReport(
        context: Context,
        officeName: String,
        summaries: List<EmployeeReportSummary>,
        yearMonth: YearMonth,
        isArabic: Boolean
    ): File {
        val fileName = "Attendance_Combined_${yearMonth.year}_${"%02d".format(yearMonth.monthValue)}.xlsx"
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsDir, fileName)

        val sheets = mutableListOf<SheetData>()
        val usedNames = mutableSetOf<String>()

        // 1. Combined Summary Worksheet: Contains the monthly employee summary at top,
        // and the comprehensive detailed attendance section with all employees' daily records below it!
        val summarySheet = buildSummaryWorksheet(
            officeName = officeName,
            summaries = summaries,
            yearMonth = yearMonth,
            isArabic = isArabic
        )
        usedNames.add(summarySheet.title.lowercase())
        sheets.add(summarySheet)

        // 2. Master All Attendance Worksheet
        val allDailySheet = buildAllEmployeesDailySheetData(
            officeName = officeName,
            summaries = summaries,
            yearMonth = yearMonth,
            isArabic = isArabic
        )
        usedNames.add(allDailySheet.title.lowercase())
        sheets.add(allDailySheet)

        // 3. Separate worksheet for each employee (EMP001–EMP008)
        for (s in summaries) {
            val empName = if (isArabic && s.employee.nameAr.isNotBlank()) s.employee.nameAr else s.employee.name
            val rawTitle = "${s.employee.id} - ${empName.take(12)}"
            val sanitized = sanitizeSheetName(rawTitle, usedNames)

            val empSheet = buildEmployeeSheetData(
                sheetTitle = sanitized,
                officeName = officeName,
                employee = s.employee,
                shift = s.assignedShift,
                records = s.records,
                yearMonth = yearMonth,
                isArabic = isArabic
            )
            sheets.add(empSheet)
        }

        writeMultiSheetXlsx(outputFile, sheets, isArabic)
        return outputFile
    }

    /**
     * ONE-PAGE monthly sheet for ALL workers (same layout as the paper attendance sheet):
     * rows = days of the month (1..30/31), and for each worker two columns: Check-in / Check-out.
     * Up to [workersPerPage] workers per sheet; each sheet is set to print on ONE landscape A4 page.
     */
    fun exportOnePageMonthlySheet(
        context: Context,
        officeName: String,
        employeeRecords: List<Pair<Employee, List<AttendanceRecord>>>,
        yearMonth: YearMonth,
        isArabic: Boolean,
        workersPerPage: Int = 8
    ): File {
        val fileName = "Attendance_AllWorkers_OnePage_${yearMonth.year}_${"%02d".format(yearMonth.monthValue)}.xlsx"
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsDir, fileName)

        val monthLabel = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}"
        val lblDate = if (isArabic) "التاريخ" else "Date"
        val lblIn = if (isArabic) "حضور" else "Check-in"
        val lblOut = if (isArabic) "انصراف" else "Check-out"
        val title = if (isArabic) "كشف حضور وانصراف الموظفين لشهر $monthLabel - $officeName"
        else "$officeName - Staff Attendance Sheet - $monthLabel"

        val pages = employeeRecords.chunked(workersPerPage.coerceAtLeast(1)).ifEmpty { listOf(emptyList()) }
        val sheets = mutableListOf<SheetData>()

        for ((pageIdx, page) in pages.withIndex()) {
            val totalCols = 1 + page.size * 2
            val rows = mutableListOf<List<StyledCell>>()
            val merges = mutableListOf<String>()

            // Row 1: title (merged across all columns)
            rows.add(List(totalCols) { i -> StyledCell(if (i == 0) title else "", STYLE_TITLE_CENTERED) })
            merges.add("A1:${getColumnLetter(totalCols - 1)}1")

            // Row 2: Date + worker names (each merged over 2 columns)
            val nameRow = mutableListOf(StyledCell(lblDate, STYLE_HEADER))
            // Row 3: Check-in / Check-out labels
            val subRow = mutableListOf(StyledCell("", STYLE_HEADER))
            merges.add("A2:A3")
            for ((i, pair) in page.withIndex()) {
                val emp = pair.first
                val name = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                nameRow.add(StyledCell(name, STYLE_NAME_HEADER))
                nameRow.add(StyledCell("", STYLE_NAME_HEADER))
                subRow.add(StyledCell(lblIn, STYLE_HEADER))
                subRow.add(StyledCell(lblOut, STYLE_HEADER))
                val c1 = getColumnLetter(1 + i * 2)
                val c2 = getColumnLetter(2 + i * 2)
                merges.add("${c1}2:${c2}2")
            }
            rows.add(nameRow)
            rows.add(subRow)

            // Per-worker lookup: date -> (first check-in, last check-out)
            val lookups = page.map { (_, recs) -> recs.groupBy { it.attendanceDate } }

            // Rows 4..: one row per day of the month
            for (day in 1..yearMonth.lengthOfMonth()) {
                val dateKey = yearMonth.atDay(day).toString() // yyyy-MM-dd
                val row = mutableListOf(StyledCell(day.toString(), STYLE_SUMMARY_LABEL))
                for (lookup in lookups) {
                    val dayRecs = lookup[dateKey].orEmpty()
                    val inMillis = dayRecs.filter { it.checkInTime > 0 }.minOfOrNull { it.checkInTime }
                    val outMillis = dayRecs.mapNotNull { it.checkOutTime }.maxOrNull()
                    val inText = inMillis?.let { TimeUtil.formatMillisToShortTime(it) } ?: ""
                    val outText = outMillis?.let { TimeUtil.formatMillisToShortTime(it) } ?: ""
                    row.add(StyledCell(inText, STYLE_CENTERED))
                    row.add(StyledCell(outText, STYLE_CENTERED))
                }
                rows.add(row)
            }

            val columns = listOf(SheetColumnDef(7.0)) + List(page.size * 2) { SheetColumnDef(12.5) }
            sheets.add(
                SheetData(
                    title = if (pages.size > 1) "$monthLabel (${pageIdx + 1})" else monthLabel,
                    columns = columns,
                    rows = rows,
                    freezeRow = 3,
                    merges = merges,
                    landscapeOnePage = true
                )
            )
        }

        writeMultiSheetXlsx(outputFile, sheets, isArabic)
        return outputFile
    }

    /**
     * Builds the detailed employee worksheet containing daily attendance history.
     */
    private fun buildEmployeeSheetData(
        sheetTitle: String,
        officeName: String,
        employee: Employee,
        shift: Shift?,
        records: List<AttendanceRecord>,
        yearMonth: YearMonth,
        isArabic: Boolean
    ): SheetData {
        // Columns configured with generous widths to ensure all times, names, shifts, and headers are fully readable
        val columns = listOf(
            SheetColumnDef(16.0), // 1. Date
            SheetColumnDef(16.0), // 2. Employee ID
            SheetColumnDef(26.0), // 3. Employee Name
            SheetColumnDef(30.0), // 4. Assigned Shift
            SheetColumnDef(18.0), // 5. Check-in Time
            SheetColumnDef(18.0), // 6. Check-out Time
            SheetColumnDef(22.0), // 7. Total Working Hours
            SheetColumnDef(16.0), // 8. Late Minutes
            SheetColumnDef(24.0), // 9. Attendance Status
            SheetColumnDef(30.0)  // 10. Check-in / Check-out Completion (Tick Mark ✓)
        )

        val rows = mutableListOf<List<StyledCell>>()

        // Header Title Block
        val displayName = if (isArabic && employee.nameAr.isNotBlank()) employee.nameAr else employee.name
        val shiftName = shift?.let { if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn } ?: employee.assignedShiftId
        val monthLabel = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}"

        rows.add(listOf(StyledCell(officeName, STYLE_TITLE)))
        rows.add(listOf(StyledCell(
            if (isArabic) "سجل الحضور والانصراف اليومي الشهري للموظف" else "Monthly Daily Attendance & Shift History",
            STYLE_SUBTITLE
        )))
        rows.add(listOf(StyledCell(
            "${if (isArabic) "الموظف:" else "Employee:"} $displayName (${employee.id})  |  ${if (isArabic) "الوردية:" else "Shift:"} $shiftName  |  ${if (isArabic) "الشهر:" else "Month:"} $monthLabel",
            STYLE_SUBTITLE
        )))
        rows.add(emptyList())

        // 10 Column Headers in EXACT order
        val headerRow = if (isArabic) {
            listOf(
                StyledCell("التاريخ", STYLE_HEADER),
                StyledCell("الرقم الوظيفي", STYLE_HEADER),
                StyledCell("اسم الموظف", STYLE_HEADER),
                StyledCell("الوردية المحددة", STYLE_HEADER),
                StyledCell("وقت الحضور", STYLE_HEADER),
                StyledCell("وقت الانصراف", STYLE_HEADER),
                StyledCell("إجمالي ساعات العمل", STYLE_HEADER),
                StyledCell("دقائق التأخير", STYLE_HEADER),
                StyledCell("حالة الحضور", STYLE_HEADER),
                StyledCell("اكتمال الحضور والانصراف (علامة الصح ✓)", STYLE_HEADER)
            )
        } else {
            listOf(
                StyledCell("Date", STYLE_HEADER),
                StyledCell("Employee ID", STYLE_HEADER),
                StyledCell("Employee Name", STYLE_HEADER),
                StyledCell("Assigned Shift", STYLE_HEADER),
                StyledCell("Check-in Time", STYLE_HEADER),
                StyledCell("Check-out Time", STYLE_HEADER),
                StyledCell("Total Working Hours", STYLE_HEADER),
                StyledCell("Late Minutes", STYLE_HEADER),
                StyledCell("Attendance Status", STYLE_HEADER),
                StyledCell("Check-in / Check-out Completion (Tick Mark ✓)", STYLE_HEADER)
            )
        }
        rows.add(headerRow)

        // Group records by date
        val recordsByDate = records.groupBy { it.attendanceDate }
        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now(TimeUtil.RIYADH_ZONE)

        var completedCount = 0
        var incompleteCount = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = date.format(TimeUtil.DATE_FORMATTER)
            val dayRecords = recordsByDate[dateStr]

            if (dayRecords.isNullOrEmpty()) {
                // Rule: If no attendance is recorded, leave the Check-in and Check-out fields blank and do not show a tick.
                val isWeekend = date.dayOfWeek.value == 5 || date.dayOfWeek.value == 6
                val statusText = when {
                    isWeekend -> if (isArabic) "عطلة نهاية الأسبوع" else "Weekend"
                    date.isAfter(today) -> if (isArabic) "قادم" else "Upcoming"
                    else -> if (isArabic) "غائب" else "Absent"
                }

                rows.add(
                    listOf(
                        StyledCell(dateStr, STYLE_CENTERED),
                        StyledCell(employee.id, STYLE_CENTERED),
                        StyledCell(displayName, STYLE_NORMAL),
                        StyledCell(shiftName, STYLE_NORMAL),
                        StyledCell("", STYLE_CENTERED), // Check-in blank
                        StyledCell("", STYLE_CENTERED), // Check-out blank
                        StyledCell("", STYLE_CENTERED), // Hours blank
                        StyledCell("", STYLE_CENTERED), // Late minutes blank
                        StyledCell(statusText, STYLE_MUTED),
                        StyledCell("", STYLE_NORMAL)    // No completion tick
                    )
                )
            } else {
                for (rec in dayRecords) {
                    val hasCompletedCheckOut = rec.checkOutTime != null && rec.checkOutTime > 0L

                    val checkInText = if (rec.checkInFormatted.isNotBlank()) {
                        rec.checkInFormatted
                    } else if (rec.checkInTime > 0L) {
                        TimeUtil.formatMillisToTime(rec.checkInTime)
                    } else {
                        ""
                    }

                    val checkOutText = if (hasCompletedCheckOut) {
                        val checkOutTimeStr = if (!rec.checkOutFormatted.isNullOrBlank()) {
                            rec.checkOutFormatted!!
                        } else {
                            TimeUtil.formatMillisToTime(rec.checkOutTime!!)
                        }
                        if (rec.checkInTime > 0L) {
                            val inDate = Instant.ofEpochMilli(rec.checkInTime).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                            val outDate = Instant.ofEpochMilli(rec.checkOutTime!!).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                            if (outDate.isAfter(inDate)) {
                                val nextDaySuffix = if (isArabic) "(اليوم التالي)" else "(+1 day)"
                                "$checkOutTimeStr $nextDaySuffix"
                            } else {
                                checkOutTimeStr
                            }
                        } else {
                            checkOutTimeStr
                        }
                    } else {
                        // Rule 6: If Check-in exists but Check-out is missing, display the Check-in time, "Incomplete" for Check-out, and no completion tick.
                        if (isArabic) "غير مكتمل" else "Incomplete"
                    }

                    val hoursText = if (hasCompletedCheckOut) {
                        if (rec.totalHours > 0.0) {
                            TimeUtil.formatHoursToDurationString(rec.totalHours, isArabic)
                        } else {
                            val calc = TimeUtil.calculateWorkingHours(rec.checkInTime, rec.checkOutTime!!)
                            TimeUtil.formatHoursToDurationString(calc, isArabic)
                        }
                    } else {
                        TimeUtil.formatLiveWorkingDuration(rec.checkInTime, System.currentTimeMillis(), isArabic)
                    }

                    val lateMinutesText = if (rec.lateMinutes > 0) {
                        "${rec.lateMinutes} ${if (isArabic) "د" else "m"}"
                    } else {
                        "0 ${if (isArabic) "د" else "m"}"
                    }

                    val statusText = if (hasCompletedCheckOut) {
                        when {
                            rec.isManualOverride -> if (isArabic) "تعديل إداري (مكتمل)" else "Admin Override (Completed)"
                            rec.lateMinutes > 0 -> if (isArabic) "متأخر (${rec.lateMinutes} د)" else "Late (${rec.lateMinutes}m)"
                            else -> if (isArabic) "في الموعد" else "On Time"
                        }
                    } else {
                        if (rec.lateMinutes > 0) {
                            if (isArabic) "متأخر (غير مكتمل)" else "Late (Incomplete)"
                        } else {
                            if (isArabic) "قيد العمل" else "In Progress"
                        }
                    }

                    // Rule 6: Include completed and incomplete attendance records. If Check-in exists but Check-out is missing, display the Check-in time, "Incomplete" for Check-out, and no completion tick.
                    val completionCell = if (hasCompletedCheckOut) {
                        completedCount++
                        StyledCell("✓", STYLE_COMPLETED_TICK)
                    } else {
                        incompleteCount++
                        StyledCell("", STYLE_NORMAL) // No completion tick mark when incomplete
                    }

                    rows.add(
                        listOf(
                            StyledCell(rec.attendanceDate, STYLE_CENTERED),
                            StyledCell(employee.id, STYLE_CENTERED),
                            StyledCell(displayName, STYLE_NORMAL),
                            StyledCell(rec.assignedShiftName.ifBlank { shiftName }, STYLE_NORMAL),
                            StyledCell(checkInText, STYLE_CENTERED),
                            StyledCell(checkOutText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                            StyledCell(hoursText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                            StyledCell(lateMinutesText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_CENTERED),
                            StyledCell(statusText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_NORMAL),
                            completionCell
                        )
                    )
                }
            }
        }

        // Monthly Summary Statistics Box at bottom
        val daysWorked = records.filter { it.checkInTime > 0 }.map { it.attendanceDate }.distinct().size
        val totalHours = records.sumOf { it.totalHours }
        val totalLate = records.sumOf { it.lateMinutes }

        val scheduledWorkDays = (1..daysInMonth).count { day ->
            val d = yearMonth.atDay(day)
            d.dayOfWeek.value != 5 && d.dayOfWeek.value != 6
        }
        val absentDays = maxOf(0, scheduledWorkDays - daysWorked)

        rows.add(emptyList())
        rows.add(listOf(StyledCell(if (isArabic) "--- ملخص الحضور الشهري للموظف ---" else "--- Monthly Attendance Summary ---", STYLE_TITLE)))
        rows.add(listOf(
            StyledCell(if (isArabic) "أيام الحضور المكتملة (حضور وانصراف)" else "Completed Attendance Days (Check-In & Out)", STYLE_SUMMARY_LABEL),
            StyledCell(completedCount.toString(), STYLE_KPI_VALUE)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "سجلات الحضور غير المكتملة" else "Incomplete Attendance Records", STYLE_SUMMARY_LABEL),
            StyledCell(incompleteCount.toString(), if (incompleteCount > 0) STYLE_INCOMPLETE else STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي أيام الحضور الفعلية" else "Total Days Worked", STYLE_SUMMARY_LABEL),
            StyledCell(daysWorked.toString(), STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "أيام الغياب التقديرية" else "Estimated Absent Days", STYLE_SUMMARY_LABEL),
            StyledCell(absentDays.toString(), STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي ساعات العمل المكتملة" else "Total Working Hours", STYLE_SUMMARY_LABEL),
            StyledCell(TimeUtil.formatHoursToDurationString(totalHours, isArabic), STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي دقائق التأخير" else "Total Late Minutes", STYLE_SUMMARY_LABEL),
            StyledCell("${totalLate}m", STYLE_CENTERED)
        ))

        return SheetData(sheetTitle, columns, rows, freezeRow = 5)
    }

    /**
     * Builds the Summary worksheet for all employees in the combined monthly workbook.
     * Contains:
     * 1. Top Section: Monthly employee summary table with totals for all employees.
     * 2. Bottom Section: Comprehensive detailed attendance table containing daily attendance records
     *    of ALL employees (EMP001 through EMP008) for the selected month with all 10 columns:
     *    Date, Employee ID, Employee Name, Assigned Shift, Check-in Time, Check-out Time,
     *    Total Working Hours, Late Minutes, Attendance Status, and Completion Tick (✓).
     */
    private fun buildSummaryWorksheet(
        officeName: String,
        summaries: List<EmployeeReportSummary>,
        yearMonth: YearMonth,
        isArabic: Boolean
    ): SheetData {
        val title = if (isArabic) "الملخص العام" else "Summary"
        val columns = listOf(
            SheetColumnDef(16.0), // 1. Date / Employee ID
            SheetColumnDef(20.0), // 2. Employee ID / Employee Name
            SheetColumnDef(28.0), // 3. Employee Name / Assigned Shift
            SheetColumnDef(30.0), // 4. Assigned Shift / Completed Days
            SheetColumnDef(22.0), // 5. Check-in Time / Incomplete Records
            SheetColumnDef(26.0), // 6. Check-out Time / Total Days Worked
            SheetColumnDef(22.0), // 7. Total Working Hours / Absent Days
            SheetColumnDef(20.0), // 8. Late Minutes / Total Hours
            SheetColumnDef(24.0), // 9. Attendance Status / Late Minutes
            SheetColumnDef(24.0)  // 10. Completion Tick (✓) / Status
        )

        val rows = mutableListOf<List<StyledCell>>()

        val monthLabel = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}"
        rows.add(listOf(StyledCell(officeName, STYLE_TITLE)))
        rows.add(listOf(StyledCell(
            if (isArabic) "التقرير الشهري الشامل لجميع الموظفين" else "Combined Monthly Attendance Report",
            STYLE_SUBTITLE
        )))
        rows.add(listOf(StyledCell(
            "${if (isArabic) "الشهر:" else "Month:"} $monthLabel  |  ${if (isArabic) "إجمالي الموظفين:" else "Total Employees:"} ${summaries.size}",
            STYLE_SUBTITLE
        )))
        rows.add(emptyList())

        // -------------------------------------------------------------
        // SECTION 1 (TOP): MONTHLY EMPLOYEE TOTALS SUMMARY
        // -------------------------------------------------------------
        rows.add(listOf(StyledCell(
            if (isArabic) "--- ملخص إجمالي الشهر لجميع الموظفين ---" else "--- Monthly Employee Summary Overview ---",
            STYLE_TITLE
        )))

        val summaryHeader = if (isArabic) {
            listOf(
                StyledCell("الرقم الوظيفي", STYLE_HEADER),
                StyledCell("اسم الموظف", STYLE_HEADER),
                StyledCell("الوردية المحددة", STYLE_HEADER),
                StyledCell("أيام الحضور المكتملة", STYLE_HEADER),
                StyledCell("سجلات غير مكتملة", STYLE_HEADER),
                StyledCell("إجمالي أيام الحضور", STYLE_HEADER),
                StyledCell("أيام الغياب التقديرية", STYLE_HEADER),
                StyledCell("إجمالي ساعات العمل", STYLE_HEADER),
                StyledCell("دقائق التأخير", STYLE_HEADER),
                StyledCell("حالة الموظف", STYLE_HEADER)
            )
        } else {
            listOf(
                StyledCell("Employee ID", STYLE_HEADER),
                StyledCell("Employee Name", STYLE_HEADER),
                StyledCell("Assigned Shift", STYLE_HEADER),
                StyledCell("Completed Days", STYLE_HEADER),
                StyledCell("Incomplete Records", STYLE_HEADER),
                StyledCell("Total Days Worked", STYLE_HEADER),
                StyledCell("Absent Days", STYLE_HEADER),
                StyledCell("Total Working Hours", STYLE_HEADER),
                StyledCell("Late Minutes", STYLE_HEADER),
                StyledCell("Status", STYLE_HEADER)
            )
        }
        rows.add(summaryHeader)

        var totalCompletedAll = 0
        var totalIncompleteAll = 0
        var totalDaysAll = 0
        var totalAbsentAll = 0
        var totalHoursAll = 0.0
        var totalLateAll = 0

        for (s in summaries) {
            val empName = if (isArabic && s.employee.nameAr.isNotBlank()) s.employee.nameAr else s.employee.name
            val shiftName = s.assignedShift?.let { if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn } ?: s.employee.assignedShiftId

            totalCompletedAll += s.completedDays
            totalIncompleteAll += s.incompleteRecords
            totalDaysAll += s.totalDaysWorked
            totalAbsentAll += s.absentDays
            totalHoursAll += s.totalWorkingHours
            totalLateAll += s.totalLateMinutes

            rows.add(
                listOf(
                    StyledCell(s.employee.id, STYLE_CENTERED),
                    StyledCell(empName, STYLE_NORMAL),
                    StyledCell(shiftName, STYLE_NORMAL),
                    StyledCell(s.completedDays.toString(), STYLE_KPI_VALUE),
                    StyledCell(s.incompleteRecords.toString(), if (s.incompleteRecords > 0) STYLE_INCOMPLETE else STYLE_CENTERED),
                    StyledCell(s.totalDaysWorked.toString(), STYLE_CENTERED),
                    StyledCell(s.absentDays.toString(), STYLE_CENTERED),
                    StyledCell(TimeUtil.formatHoursToDurationString(s.totalWorkingHours, isArabic), STYLE_CENTERED),
                    StyledCell(s.totalLateMinutes.toString(), STYLE_CENTERED),
                    StyledCell(
                        if (s.employee.isActive) (if (isArabic) "نشط" else "Active") else (if (isArabic) "غير نشط" else "Inactive"),
                        STYLE_CENTERED
                    )
                )
            )
        }

        // Totals Row
        rows.add(emptyList())
        rows.add(
            listOf(
                StyledCell(if (isArabic) "الإجمالي الكلي" else "Total / Summary", STYLE_SUMMARY_LABEL),
                StyledCell("${summaries.size} ${if (isArabic) "موظف" else "Employees"}", STYLE_SUMMARY_LABEL),
                StyledCell("", STYLE_SUMMARY_LABEL),
                StyledCell(totalCompletedAll.toString(), STYLE_KPI_VALUE),
                StyledCell(totalIncompleteAll.toString(), if (totalIncompleteAll > 0) STYLE_INCOMPLETE else STYLE_CENTERED),
                StyledCell(totalDaysAll.toString(), STYLE_CENTERED),
                StyledCell(totalAbsentAll.toString(), STYLE_CENTERED),
                StyledCell(TimeUtil.formatHoursToDurationString(totalHoursAll, isArabic), STYLE_KPI_VALUE),
                StyledCell(totalLateAll.toString(), STYLE_CENTERED),
                StyledCell("", STYLE_SUMMARY_LABEL)
            )
        )

        // -------------------------------------------------------------
        // SECTION 2 (BELOW): DETAILED DAILY ATTENDANCE FOR ALL EMPLOYEES
        // -------------------------------------------------------------
        rows.add(emptyList())
        rows.add(emptyList())
        rows.add(listOf(StyledCell(
            if (isArabic) "--- سجل الحضور والانصراف اليومي التفصيلي لجميع الموظفين ---" else "--- Detailed Daily Attendance Records (All Employees) ---",
            STYLE_TITLE
        )))
        rows.add(listOf(StyledCell(
            if (isArabic)
                "تفاصيل الحضور والانصراف اليومية، أوقات الدخول والخروج، وساعات العمل للشهر المحدد مع علامة الصح (✓)"
            else
                "Daily Check-in, Check-out, and Working Hours for the Selected Month with Completion Tick (✓)",
            STYLE_SUBTITLE
        )))
        rows.add(emptyList())

        // 10 Detailed Column Headers
        val detailedHeader = if (isArabic) {
            listOf(
                StyledCell("التاريخ", STYLE_HEADER),
                StyledCell("الرقم الوظيفي", STYLE_HEADER),
                StyledCell("اسم الموظف", STYLE_HEADER),
                StyledCell("الوردية المحددة", STYLE_HEADER),
                StyledCell("وقت الحضور", STYLE_HEADER),
                StyledCell("وقت الانصراف", STYLE_HEADER),
                StyledCell("إجمالي ساعات العمل", STYLE_HEADER),
                StyledCell("دقائق التأخير", STYLE_HEADER),
                StyledCell("حالة الحضور", STYLE_HEADER),
                StyledCell("اكتمال الحضور والانصراف (✓)", STYLE_HEADER)
            )
        } else {
            listOf(
                StyledCell("Date", STYLE_HEADER),
                StyledCell("Employee ID", STYLE_HEADER),
                StyledCell("Employee Name", STYLE_HEADER),
                StyledCell("Assigned Shift", STYLE_HEADER),
                StyledCell("Check-in Time", STYLE_HEADER),
                StyledCell("Check-out Time", STYLE_HEADER),
                StyledCell("Total Working Hours", STYLE_HEADER),
                StyledCell("Late Minutes", STYLE_HEADER),
                StyledCell("Attendance Status", STYLE_HEADER),
                StyledCell("Completion Tick (✓)", STYLE_HEADER)
            )
        }
        rows.add(detailedHeader)

        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now(TimeUtil.RIYADH_ZONE)

        var detailedCompletedCount = 0
        var detailedIncompleteCount = 0
        var detailedTotalHours = 0.0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = date.format(TimeUtil.DATE_FORMATTER)
            val isWeekend = date.dayOfWeek.value == 5 || date.dayOfWeek.value == 6

            for (s in summaries) {
                val emp = s.employee
                val displayName = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                val shift = s.assignedShift
                val shiftName = shift?.let { if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn } ?: emp.assignedShiftId

                val dayRecords = s.records.filter { it.attendanceDate == dateStr }

                if (dayRecords.isEmpty()) {
                    val statusText = when {
                        isWeekend -> if (isArabic) "عطلة نهاية الأسبوع" else "Weekend"
                        date.isAfter(today) -> if (isArabic) "قادم" else "Upcoming"
                        else -> if (isArabic) "غائب" else "Absent"
                    }

                    rows.add(
                        listOf(
                            StyledCell(dateStr, STYLE_CENTERED),
                            StyledCell(emp.id, STYLE_CENTERED),
                            StyledCell(displayName, STYLE_NORMAL),
                            StyledCell(shiftName, STYLE_NORMAL),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell(statusText, STYLE_MUTED),
                            StyledCell("", STYLE_NORMAL)
                        )
                    )
                } else {
                    for (rec in dayRecords) {
                        val hasCompletedCheckOut = rec.checkOutTime != null && rec.checkOutTime > 0L

                        val checkInText = if (rec.checkInFormatted.isNotBlank()) {
                            rec.checkInFormatted
                        } else if (rec.checkInTime > 0L) {
                            TimeUtil.formatMillisToTime(rec.checkInTime)
                        } else {
                            ""
                        }

                        val checkOutText = if (hasCompletedCheckOut) {
                            val checkOutTimeStr = if (!rec.checkOutFormatted.isNullOrBlank()) {
                                rec.checkOutFormatted!!
                            } else {
                                TimeUtil.formatMillisToTime(rec.checkOutTime!!)
                            }
                            // Correctly show next-day Check-outs for overnight shifts
                            if (rec.checkInTime > 0L) {
                                val inDate = Instant.ofEpochMilli(rec.checkInTime).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                                val outDate = Instant.ofEpochMilli(rec.checkOutTime!!).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                                if (outDate.isAfter(inDate)) {
                                    val nextDaySuffix = if (isArabic) "(اليوم التالي)" else "(+1 day)"
                                    "$checkOutTimeStr $nextDaySuffix"
                                } else {
                                    checkOutTimeStr
                                }
                            } else {
                                checkOutTimeStr
                            }
                        } else {
                            if (isArabic) "غير مكتمل" else "Incomplete"
                        }

                        val hoursText = if (hasCompletedCheckOut) {
                            detailedTotalHours += rec.totalHours
                            if (rec.totalHours > 0.0) {
                                TimeUtil.formatHoursToDurationString(rec.totalHours, isArabic)
                            } else {
                                val calc = TimeUtil.calculateWorkingHours(rec.checkInTime, rec.checkOutTime!!)
                                TimeUtil.formatHoursToDurationString(calc, isArabic)
                            }
                        } else {
                            TimeUtil.formatLiveWorkingDuration(rec.checkInTime, System.currentTimeMillis(), isArabic)
                        }

                        val lateMinutesText = if (rec.lateMinutes > 0) {
                            "${rec.lateMinutes} ${if (isArabic) "د" else "m"}"
                        } else {
                            "0 ${if (isArabic) "د" else "m"}"
                        }

                        val statusText = if (hasCompletedCheckOut) {
                            when {
                                rec.isManualOverride -> if (isArabic) "تعديل إداري (مكتمل)" else "Admin Override (Completed)"
                                rec.lateMinutes > 0 -> if (isArabic) "متأخر (${rec.lateMinutes} د)" else "Late (${rec.lateMinutes}m)"
                                else -> if (isArabic) "في الموعد" else "On Time"
                            }
                        } else {
                            if (rec.lateMinutes > 0) {
                                if (isArabic) "متأخر (غير مكتمل)" else "Late (Incomplete)"
                            } else {
                                if (isArabic) "قيد العمل" else "In Progress"
                            }
                        }

                        val completionCell = if (hasCompletedCheckOut) {
                            detailedCompletedCount++
                            StyledCell("✓", STYLE_COMPLETED_TICK)
                        } else {
                            detailedIncompleteCount++
                            StyledCell("", STYLE_NORMAL)
                        }

                        rows.add(
                            listOf(
                                StyledCell(rec.attendanceDate, STYLE_CENTERED),
                                StyledCell(emp.id, STYLE_CENTERED),
                                StyledCell(displayName, STYLE_NORMAL),
                                StyledCell(rec.assignedShiftName.ifBlank { shiftName }, STYLE_NORMAL),
                                StyledCell(checkInText, STYLE_CENTERED),
                                StyledCell(checkOutText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                                StyledCell(hoursText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                                StyledCell(lateMinutesText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_CENTERED),
                                StyledCell(statusText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_NORMAL),
                                completionCell
                            )
                        )
                    }
                }
            }
        }

        // Detailed Section Footer
        rows.add(emptyList())
        rows.add(listOf(StyledCell(if (isArabic) "--- ملخص إجمالي الحضور اليومي المفصل ---" else "--- Detailed Attendance Totals Summary ---", STYLE_TITLE)))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي سجلات الحضور المكتملة (✓)" else "Total Completed Attendance Records (✓)", STYLE_SUMMARY_LABEL),
            StyledCell(detailedCompletedCount.toString(), STYLE_KPI_VALUE)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "سجلات الحضور غير المكتملة" else "Incomplete Attendance Records", STYLE_SUMMARY_LABEL),
            StyledCell(detailedIncompleteCount.toString(), if (detailedIncompleteCount > 0) STYLE_INCOMPLETE else STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي ساعات العمل المكتملة" else "Total Working Hours Completed", STYLE_SUMMARY_LABEL),
            StyledCell(TimeUtil.formatHoursToDurationString(detailedTotalHours, isArabic), STYLE_KPI_VALUE)
        ))

        return SheetData(title, columns, rows, freezeRow = 6)
    }

    /**
     * Builds the unified master daily attendance worksheet containing daily history for all employees.
     */
    private fun buildAllEmployeesDailySheetData(
        officeName: String,
        summaries: List<EmployeeReportSummary>,
        yearMonth: YearMonth,
        isArabic: Boolean
    ): SheetData {
        val title = if (isArabic) "سجل الحضور الشامل" else "All Attendance"
        val columns = listOf(
            SheetColumnDef(16.0), // 1. Date
            SheetColumnDef(16.0), // 2. Employee ID
            SheetColumnDef(26.0), // 3. Employee Name
            SheetColumnDef(30.0), // 4. Assigned Shift
            SheetColumnDef(18.0), // 5. Check-in Time
            SheetColumnDef(22.0), // 6. Check-out Time
            SheetColumnDef(22.0), // 7. Total Working Hours
            SheetColumnDef(16.0), // 8. Late Minutes
            SheetColumnDef(24.0), // 9. Attendance Status
            SheetColumnDef(26.0)  // 10. Check-in / Check-out Completion (Tick Mark ✓)
        )

        val rows = mutableListOf<List<StyledCell>>()

        val monthLabel = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}"
        rows.add(listOf(StyledCell(officeName, STYLE_TITLE)))
        rows.add(listOf(StyledCell(
            if (isArabic) "سجل الحضور والانصراف اليومي الموحد لجميع الموظفين" else "Unified Daily Attendance & Duty Hours Report (All Employees)",
            STYLE_SUBTITLE
        )))
        rows.add(listOf(StyledCell(
            "${if (isArabic) "الشهر:" else "Month:"} $monthLabel  |  ${if (isArabic) "إجمالي الموظفين:" else "Total Employees:"} ${summaries.size}",
            STYLE_SUBTITLE
        )))
        rows.add(emptyList())

        // Header Row
        val headerRow = if (isArabic) {
            listOf(
                StyledCell("التاريخ", STYLE_HEADER),
                StyledCell("الرقم الوظيفي", STYLE_HEADER),
                StyledCell("اسم الموظف", STYLE_HEADER),
                StyledCell("الوردية المحددة", STYLE_HEADER),
                StyledCell("وقت الحضور", STYLE_HEADER),
                StyledCell("وقت الانصراف", STYLE_HEADER),
                StyledCell("إجمالي ساعات العمل", STYLE_HEADER),
                StyledCell("دقائق التأخير", STYLE_HEADER),
                StyledCell("حالة الحضور", STYLE_HEADER),
                StyledCell("اكتمال الحضور (✓)", STYLE_HEADER)
            )
        } else {
            listOf(
                StyledCell("Date", STYLE_HEADER),
                StyledCell("Employee ID", STYLE_HEADER),
                StyledCell("Employee Name", STYLE_HEADER),
                StyledCell("Assigned Shift", STYLE_HEADER),
                StyledCell("Check-in Time", STYLE_HEADER),
                StyledCell("Check-out Time", STYLE_HEADER),
                StyledCell("Total Working Hours", STYLE_HEADER),
                StyledCell("Late Minutes", STYLE_HEADER),
                StyledCell("Attendance Status", STYLE_HEADER),
                StyledCell("Check-in / Check-out Completion (Tick Mark ✓)", STYLE_HEADER)
            )
        }
        rows.add(headerRow)

        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now(TimeUtil.RIYADH_ZONE)

        var totalCompletedRows = 0
        var totalIncompleteRows = 0
        var totalDutyHoursAll = 0.0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = date.format(TimeUtil.DATE_FORMATTER)
            val isWeekend = date.dayOfWeek.value == 5 || date.dayOfWeek.value == 6

            for (s in summaries) {
                val emp = s.employee
                val displayName = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                val shift = s.assignedShift
                val shiftName = shift?.let { if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.nameEn } ?: emp.assignedShiftId

                val dayRecords = s.records.filter { it.attendanceDate == dateStr }

                if (dayRecords.isEmpty()) {
                    val statusText = when {
                        isWeekend -> if (isArabic) "عطلة نهاية الأسبوع" else "Weekend"
                        date.isAfter(today) -> if (isArabic) "قادم" else "Upcoming"
                        else -> if (isArabic) "غائب" else "Absent"
                    }

                    rows.add(
                        listOf(
                            StyledCell(dateStr, STYLE_CENTERED),
                            StyledCell(emp.id, STYLE_CENTERED),
                            StyledCell(displayName, STYLE_NORMAL),
                            StyledCell(shiftName, STYLE_NORMAL),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell("", STYLE_CENTERED),
                            StyledCell(statusText, STYLE_MUTED),
                            StyledCell("", STYLE_NORMAL)
                        )
                    )
                } else {
                    for (rec in dayRecords) {
                        val hasCompletedCheckOut = rec.checkOutTime != null && rec.checkOutTime > 0L

                        val checkInText = if (rec.checkInFormatted.isNotBlank()) {
                            rec.checkInFormatted
                        } else if (rec.checkInTime > 0L) {
                            TimeUtil.formatMillisToTime(rec.checkInTime)
                        } else {
                            ""
                        }

                        val checkOutText = if (hasCompletedCheckOut) {
                            val checkOutTimeStr = if (!rec.checkOutFormatted.isNullOrBlank()) {
                                rec.checkOutFormatted!!
                            } else {
                                TimeUtil.formatMillisToTime(rec.checkOutTime!!)
                            }
                            if (rec.checkInTime > 0L) {
                                val inDate = Instant.ofEpochMilli(rec.checkInTime).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                                val outDate = Instant.ofEpochMilli(rec.checkOutTime!!).atZone(TimeUtil.RIYADH_ZONE).toLocalDate()
                                if (outDate.isAfter(inDate)) {
                                    val nextDaySuffix = if (isArabic) "(اليوم التالي)" else "(+1 day)"
                                    "$checkOutTimeStr $nextDaySuffix"
                                } else {
                                    checkOutTimeStr
                                }
                            } else {
                                checkOutTimeStr
                            }
                        } else {
                            if (isArabic) "غير مكتمل" else "Incomplete"
                        }

                        val hoursText = if (hasCompletedCheckOut) {
                            totalDutyHoursAll += rec.totalHours
                            if (rec.totalHours > 0.0) {
                                TimeUtil.formatHoursToDurationString(rec.totalHours, isArabic)
                            } else {
                                val calc = TimeUtil.calculateWorkingHours(rec.checkInTime, rec.checkOutTime!!)
                                TimeUtil.formatHoursToDurationString(calc, isArabic)
                            }
                        } else {
                            TimeUtil.formatLiveWorkingDuration(rec.checkInTime, System.currentTimeMillis(), isArabic)
                        }

                        val lateMinutesText = if (rec.lateMinutes > 0) {
                            "${rec.lateMinutes} ${if (isArabic) "د" else "m"}"
                        } else {
                            "0 ${if (isArabic) "د" else "m"}"
                        }

                        val statusText = if (hasCompletedCheckOut) {
                            when {
                                rec.isManualOverride -> if (isArabic) "تعديل إداري (مكتمل)" else "Admin Override (Completed)"
                                rec.lateMinutes > 0 -> if (isArabic) "متأخر (${rec.lateMinutes} د)" else "Late (${rec.lateMinutes}m)"
                                else -> if (isArabic) "في الموعد" else "On Time"
                            }
                        } else {
                            if (rec.lateMinutes > 0) {
                                if (isArabic) "متأخر (غير مكتمل)" else "Late (Incomplete)"
                            } else {
                                if (isArabic) "قيد العمل" else "In Progress"
                            }
                        }

                        val completionCell = if (hasCompletedCheckOut) {
                            totalCompletedRows++
                            StyledCell("✓", STYLE_COMPLETED_TICK)
                        } else {
                            totalIncompleteRows++
                            StyledCell("", STYLE_NORMAL)
                        }

                        rows.add(
                            listOf(
                                StyledCell(rec.attendanceDate, STYLE_CENTERED),
                                StyledCell(emp.id, STYLE_CENTERED),
                                StyledCell(displayName, STYLE_NORMAL),
                                StyledCell(rec.assignedShiftName.ifBlank { shiftName }, STYLE_NORMAL),
                                StyledCell(checkInText, STYLE_CENTERED),
                                StyledCell(checkOutText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                                StyledCell(hoursText, if (hasCompletedCheckOut) STYLE_CENTERED else STYLE_INCOMPLETE),
                                StyledCell(lateMinutesText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_CENTERED),
                                StyledCell(statusText, if (rec.lateMinutes > 0) STYLE_INCOMPLETE else STYLE_NORMAL),
                                completionCell
                            )
                        )
                    }
                }
            }
        }

        // Summary footer
        rows.add(emptyList())
        rows.add(listOf(StyledCell(if (isArabic) "--- إحصائيات السجل الموحد ---" else "--- Summary Statistics ---", STYLE_TITLE)))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي سجلات الحضور المكتملة (✓)" else "Total Completed Attendance Records (✓)", STYLE_SUMMARY_LABEL),
            StyledCell(totalCompletedRows.toString(), STYLE_KPI_VALUE)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "سجلات الحضور غير المكتملة" else "Incomplete Attendance Records", STYLE_SUMMARY_LABEL),
            StyledCell(totalIncompleteRows.toString(), if (totalIncompleteRows > 0) STYLE_INCOMPLETE else STYLE_CENTERED)
        ))
        rows.add(listOf(
            StyledCell(if (isArabic) "إجمالي ساعات العمل الكلية" else "Total Duty Hours Worked", STYLE_SUMMARY_LABEL),
            StyledCell(TimeUtil.formatHoursToDurationString(totalDutyHoursAll, isArabic), STYLE_KPI_VALUE)
        ))

        return SheetData(title, columns, rows, freezeRow = 5)
    }

    private fun sanitizeSheetName(name: String, existingNames: MutableSet<String>): String {
        val clean = name.replace("[:\\\\/?*\\[\\]]".toRegex(), "").trim().take(28)
        var candidate = if (clean.isBlank()) "Employee" else clean
        var counter = 1
        while (existingNames.contains(candidate.lowercase())) {
            val suffix = "_$counter"
            candidate = candidate.take(31 - suffix.length) + suffix
            counter++
        }
        existingNames.add(candidate.lowercase())
        return candidate
    }

    /**
     * Generates a fully compliant OpenXML (.xlsx) ZIP archive.
     * Guaranteed to open natively in Microsoft Excel, LibreOffice, Google Sheets.
     */
    private fun writeMultiSheetXlsx(file: File, sheets: List<SheetData>, isArabic: Boolean) {
        val sharedStrings = mutableListOf<String>()
        val stringMap = HashMap<String, Int>()

        fun getStringIndex(str: String): Int {
            return stringMap.getOrPut(str) {
                val idx = sharedStrings.size
                sharedStrings.add(str)
                idx
            }
        }

        // Index all strings
        for (sheet in sheets) {
            for (row in sheet.rows) {
                for (cell in row) {
                    if (cell.text.isNotEmpty()) {
                        getStringIndex(cell.text)
                    }
                }
            }
        }

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypesXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
                for (i in 1..sheets.size) {
                    append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                }
                append("""</Types>""")
            }
            zip.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            zip.write(rootRelsXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRelsXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                for (i in 1..sheets.size) {
                    append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
                }
                val stylesId = sheets.size + 1
                val sstId = sheets.size + 2
                append("""<Relationship Id="rId$stylesId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("""<Relationship Id="rId$sstId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>""")
                append("""</Relationships>""")
            }
            zip.write(wbRelsXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            val wbXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
                append("""<sheets>""")
                for ((idx, sheet) in sheets.withIndex()) {
                    val sId = idx + 1
                    val safeName = escapeXml(sheet.title.take(31))
                    append("""<sheet name="$safeName" sheetId="$sId" r:id="rId$sId"/>""")
                }
                append("""</sheets>""")
                append("""</workbook>""")
            }
            zip.write(wbXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="8">
<font><sz val="11"/><name val="Calibri"/><color rgb="FF1F2937"/></font>
<font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFFFFFFF"/></font>
<font><b/><sz val="13"/><name val="Calibri"/><color rgb="FF047857"/></font>
<font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFB45309"/></font>
<font><b/><sz val="14"/><name val="Calibri"/><color rgb="FF1E3A8A"/></font>
<font><sz val="10"/><name val="Calibri"/><color rgb="FF9CA3AF"/></font>
<font><b/><sz val="11"/><name val="Calibri"/><color rgb="FF1F2937"/></font>
<font><i/><sz val="10"/><name val="Calibri"/><color rgb="FF6B7280"/></font>
</fonts>
<fills count="7">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFECFDF5"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF3F4F6"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/></patternFill></fill>
</fills>
<borders count="2">
<border><left/><right/><top/><bottom/></border>
<border><left style="thin"><color rgb="FFD1D5DB"/></left><right style="thin"><color rgb="FFD1D5DB"/></right><top style="thin"><color rgb="FFD1D5DB"/></top><bottom style="thin"><color rgb="FFD1D5DB"/></bottom></border>
</borders>
<cellStyleXfs count="1">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
</cellStyleXfs>
<cellXfs count="12">
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="6" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="7" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="6" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="6" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" shrinkToFit="1"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
</styleSheet>"""
            zip.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 6. xl/sharedStrings.xml
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val sstXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
                for (s in sharedStrings) {
                    append("<si><t>${escapeXml(s)}</t></si>")
                }
                append("</sst>")
            }
            zip.write(sstXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 7. Worksheets
            for ((idx, sheet) in sheets.withIndex()) {
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${idx + 1}.xml"))
                val wsXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                    if (sheet.landscapeOnePage) {
                        append("""<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>""")
                    }

                    // Sheet Views (RTL for Arabic and freeze pane if configured)
                    val rtlAttr = if (isArabic) """ rightToLeft="1"""" else ""
                    if (sheet.freezeRow != null && sheet.freezeRow > 0) {
                        val nextRow = sheet.freezeRow + 1
                        append("""<sheetViews><sheetView workbookViewId="0"$rtlAttr><pane ySplit="${sheet.freezeRow}" topLeftCell="A$nextRow" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
                    } else {
                        if (isArabic) {
                            append("""<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>""")
                        } else {
                            append("""<sheetViews><sheetView workbookViewId="0"/></sheetViews>""")
                        }
                    }

                    // Column Widths
                    if (sheet.columns.isNotEmpty()) {
                        append("""<cols>""")
                        for ((cIdx, col) in sheet.columns.withIndex()) {
                            val colNum = cIdx + 1
                            append("""<col min="$colNum" max="$colNum" width="${col.width}" customWidth="1"/>""")
                        }
                        append("""</cols>""")
                    }

                    append("""<sheetData>""")
                    for ((rowIdx, row) in sheet.rows.withIndex()) {
                        val rowNum = rowIdx + 1
                        if (row.isEmpty()) {
                            append("""<row r="$rowNum"/>""")
                        } else {
                            append("""<row r="$rowNum">""")
                            for ((colIdx, cell) in row.withIndex()) {
                                val colLetter = getColumnLetter(colIdx)
                                val cellRef = "$colLetter$rowNum"
                                val styleAttr = if (cell.style > 0) """ s="${cell.style}"""" else ""

                                if (cell.text.isNotEmpty()) {
                                    val sstIndex = getStringIndex(cell.text)
                                    append("""<c r="$cellRef"$styleAttr t="s"><v>$sstIndex</v></c>""")
                                } else {
                                    // Keep border/fill intact for empty cells in table
                                    append("""<c r="$cellRef"$styleAttr/>""")
                                }
                            }
                            append("""</row>""")
                        }
                    }
                    append("""</sheetData>""")
                    if (sheet.merges.isNotEmpty()) {
                        append("""<mergeCells count="${sheet.merges.size}">""")
                        for (m in sheet.merges) append("""<mergeCell ref="$m"/>""")
                        append("""</mergeCells>""")
                    }
                    if (sheet.landscapeOnePage) {
                        append("""<pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.3" footer="0.3"/>""")
                        append("""<pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="1"/>""")
                    }
                    append("""</worksheet>""")
                }
                zip.write(wsXml.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun getColumnLetter(colIndex: Int): String {
        var num = colIndex
        val sb = StringBuilder()
        while (num >= 0) {
            val rem = num % 26
            sb.insert(0, ('A'.code + rem).toChar())
            num = num / 26 - 1
        }
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
