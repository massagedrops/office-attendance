package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.Shift
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import com.example.util.Localization
import com.example.util.SoundUtil
import com.example.util.TimeUtil
import com.example.util.WhatsAppUtil
import com.example.util.XlsxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicBoolean

enum class AppTab {
    DASHBOARD,
    QR_SCANNER,
    EMPLOYEES,
    SHIFTS,
    ATTENDANCE_RECORDS,
    MONTHLY_REPORTS,
    EXCEL_EXPORT,
    SETTINGS
}

data class DashboardStats(
    val totalEmployees: Int = 0,
    val checkedInCount: Int = 0,
    val checkedOutCount: Int = 0,
    val todayAttendanceCount: Int = 0,
    val lateArrivalsCount: Int = 0,
    val absentCount: Int = 0,
    val totalHoursToday: Double = 0.0,
    val checkedInList: List<AttendanceRecord> = emptyList(),
    val absentEmployeesList: List<Employee> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(
            employeeDao = db.employeeDao(),
            shiftDao = db.shiftDao(),
            attendanceDao = db.attendanceDao(),
            settingsDao = db.settingsDao(),
            dailyShiftAssignmentDao = db.dailyShiftAssignmentDao()
        )
        // Ensure seeding on first run
        viewModelScope.launch(Dispatchers.IO) {
            val emps = db.employeeDao().getAllEmployees()
            if (emps.isEmpty()) {
                AppDatabase.seedInitialData(db)
            }
        }
    }

    // Active Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Language / RTL
    private val _isArabic = MutableStateFlow(true)
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    fun toggleLanguage() {
        val newLang = !_isArabic.value
        _isArabic.value = newLang
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings() ?: AppSettings()
            repository.saveSettings(current.copy(language = if (newLang) "ar" else "en"))
        }
    }

    // Admin Authentication
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _showAdminAuthDialog = MutableStateFlow(false)
    val showAdminAuthDialog: StateFlow<Boolean> = _showAdminAuthDialog.asStateFlow()

    private var pendingAdminAction: (() -> Unit)? = null

    fun requestAdminAuth(onSuccess: () -> Unit) {
        if (_isAdminUnlocked.value) {
            onSuccess()
        } else {
            pendingAdminAction = onSuccess
            _showAdminAuthDialog.value = true
        }
    }

    fun dismissAdminAuthDialog() {
        _showAdminAuthDialog.value = false
        pendingAdminAction = null
    }

    fun verifyAdminPin(enteredPin: String): Boolean {
        var correctPin = "1234"
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repository.getSettings()
            if (settings != null) {
                correctPin = settings.adminPin
            }
        }
        // Also check synchronously against last known or 1234
        val matches = enteredPin == correctPin || enteredPin == (settingsFlow.value?.adminPin ?: "1234")
        if (matches) {
            _isAdminUnlocked.value = true
            _showAdminAuthDialog.value = false
            pendingAdminAction?.invoke()
            pendingAdminAction = null
            return true
        }
        return false
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    // Room Database Flows
    val employeesFlow: StateFlow<List<Employee>> = repository.allEmployeesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftsFlow: StateFlow<List<Shift>> = repository.allShiftsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecordsFlow: StateFlow<List<AttendanceRecord>> = repository.allRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsFlow: StateFlow<AppSettings?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Sync language from saved settings
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                if (settings != null) {
                    _isArabic.value = settings.language == "ar"
                }
            }
        }
    }

    // Dashboard Statistics Calculation
    val dashboardStats: StateFlow<DashboardStats> = combine(
        employeesFlow,
        attendanceRecordsFlow,
        shiftsFlow
    ) { employees, records, _ ->
        val today = TimeUtil.todayDateString()
        val todayRecords = records.filter { it.attendanceDate == today }

        val activeEmployees = employees.filter { it.isActive }
        val checkedInList = todayRecords.filter { it.checkOutTime == null }
        val checkedInCount = checkedInList.size

        // Checked out count: employees who checked in today AND completed checkout
        val checkedOutCount = todayRecords.filter { it.checkOutTime != null }.map { it.employeeId }.distinct().size

        // Total employees who have attended today
        val todayAttendedEmpIds = todayRecords.map { it.employeeId }.toSet()
        val todayAttendanceCount = todayAttendedEmpIds.size

        // Late arrivals today
        val lateCount = todayRecords.count { it.lateMinutes > 0 }

        // Absent today: active employees with a shift who haven't checked in yet today
        val absentList = activeEmployees.filter { emp -> !todayAttendedEmpIds.contains(emp.id) }

        // Total hours worked today
        val totalHours = todayRecords.sumOf { it.totalHours }

        DashboardStats(
            totalEmployees = activeEmployees.size,
            checkedInCount = checkedInCount,
            checkedOutCount = checkedOutCount,
            todayAttendanceCount = todayAttendanceCount,
            lateArrivalsCount = lateCount,
            absentCount = absentList.size,
            totalHoursToday = totalHours,
            checkedInList = checkedInList,
            absentEmployeesList = absentList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // QR Code Scanning
    private val isProcessingScan = AtomicBoolean(false)
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    fun processScannedQr(code: String) {
        // Prevent continuous camera frames from launching concurrent jobs or re-triggering while a modal is active
        if (_scanResult.value != null || !isProcessingScan.compareAndSet(false, true)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.processQrScan(code)
                _scanResult.value = result

                withContext(Dispatchers.Main) {
                    val context = getApplication<Application>().applicationContext
                    when (result) {
                        is ScanResult.PendingConfirmation,
                        is ScanResult.SuccessCheckIn,
                        is ScanResult.SuccessCheckOut -> {
                            SoundUtil.playSuccessFeedback(context)
                        }
                        is ScanResult.DuplicateWarning,
                        is ScanResult.InactiveEmployee,
                        is ScanResult.NotFound,
                        is ScanResult.Error -> {
                            SoundUtil.playWarningFeedback(context)
                        }
                    }
                }
            } finally {
                isProcessingScan.set(false)
            }
        }
    }

    fun confirmAttendance(pending: ScanResult.PendingConfirmation) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.confirmPendingAttendance(pending)
            _scanResult.value = result

            withContext(Dispatchers.Main) {
                val context = getApplication<Application>().applicationContext
                when (result) {
                    is ScanResult.SuccessCheckIn, is ScanResult.SuccessCheckOut -> {
                        SoundUtil.playSuccessFeedback(context)
                    }
                    else -> {
                        SoundUtil.playWarningFeedback(context)
                    }
                }
            }
        }
    }

    fun dismissScanResult() {
        _scanResult.value = null
        isProcessingScan.set(false)
        repository.resetScanLock()
    }

    // Employee CRUD
    fun saveEmployee(employee: Employee) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertEmployee(employee)
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployee(employee)
        }
    }

    fun regenerateEmployeeQr(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.regenerateEmployeeQr(employeeId)
        }
    }

    // Shift CRUD
    fun saveShift(shift: Shift) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertShift(shift)
        }
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteShift(shift)
        }
    }

    fun assignDailyShiftOverride(employeeId: String, date: String, shiftId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDailyShiftOverride(employeeId, date, shiftId)
        }
    }

    // Attendance Record Editing / Override
    fun saveAttendanceCorrection(record: AttendanceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAttendanceRecord(record)
        }
    }

    fun deleteAttendanceRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttendanceRecord(id)
        }
    }

    // Settings Updates
    fun updateOfficeSettings(
        officeName: String,
        adminPin: String,
        gracePeriod: Int,
        bossWhatsAppNumber: String = settingsFlow.value?.bossWhatsAppNumber ?: ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings() ?: AppSettings()
            repository.saveSettings(
                current.copy(
                    officeName = officeName,
                    adminPin = adminPin,
                    lateGraceMinutes = gracePeriod,
                    bossWhatsAppNumber = bossWhatsAppNumber
                )
            )
        }
    }

    fun updateBossWhatsAppNumber(bossWhatsAppNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings() ?: AppSettings()
            repository.saveSettings(
                current.copy(
                    bossWhatsAppNumber = bossWhatsAppNumber.trim()
                )
            )
        }
    }

    fun notifyBossOnWhatsApp(
        context: Context,
        employeeName: String,
        time: String,
        date: String = "",
        isCheckIn: Boolean
    ): Boolean {
        val bossNumber = settingsFlow.value?.bossWhatsAppNumber ?: ""
        val message = if (isCheckIn) {
            WhatsAppUtil.buildCheckInMessage(
                employeeName = employeeName,
                checkInTime = time,
                date = date,
                isArabic = _isArabic.value
            )
        } else {
            WhatsAppUtil.buildCheckOutMessage(
                employeeName = employeeName,
                checkOutTime = time,
                date = date,
                isArabic = _isArabic.value
            )
        }
        return WhatsAppUtil.openWhatsApp(context, bossNumber, message)
    }

    // Excel Export & WhatsApp Sharing
    private val _isGeneratingExcel = MutableStateFlow(false)
    val isGeneratingExcel: StateFlow<Boolean> = _isGeneratingExcel.asStateFlow()

    private val _lastGeneratedFile = MutableStateFlow<File?>(null)
    val lastGeneratedFile: StateFlow<File?> = _lastGeneratedFile.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    // Separate Excel file for EACH worker (bulk export)
    private val _generatedFiles = MutableStateFlow<List<File>>(emptyList())
    val generatedFiles: StateFlow<List<File>> = _generatedFiles.asStateFlow()

    /**
     * Generates one separate .xlsx attendance report per employee for the selected month.
     * Each file contains only that worker's attendance.
     */
    fun generateAllIndividualExcelReports(yearMonth: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingExcel.value = true
            _exportMessage.value = null
            _lastGeneratedFile.value = null
            _generatedFiles.value = emptyList()
            try {
                val context = getApplication<Application>().applicationContext
                val employees = repository.getAllEmployees()
                val settings = repository.getSettings()
                val officeName = if (_isArabic.value) {
                    settings?.officeNameAr ?: "إدارة حضور المكتب"
                } else {
                    settings?.officeName ?: "Office Attendance Manager"
                }

                val files = mutableListOf<File>()
                for (emp in employees) {
                    val shift = repository.getShiftById(emp.assignedShiftId)
                    val records = repository.getMonthlyRecordsForEmployee(emp.id, yearMonth)
                    files += XlsxExporter.exportIndividualReport(
                        context = context,
                        officeName = officeName,
                        employee = emp,
                        shift = shift,
                        records = records,
                        yearMonth = yearMonth,
                        isArabic = _isArabic.value,
                        includeNameInFileName = true
                    )
                }
                _generatedFiles.value = files
                _exportMessage.value = "${files.size} files"
            } catch (e: Exception) {
                _exportMessage.value = "Error: ${e.message}"
            } finally {
                _isGeneratingExcel.value = false
            }
        }
    }

    /**
     * One-page monthly sheet containing ALL workers together
     * (rows = days, two columns per worker: check-in / check-out).
     */
    fun generateOnePageMonthlySheet(yearMonth: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingExcel.value = true
            _exportMessage.value = null
            _generatedFiles.value = emptyList()
            try {
                val context = getApplication<Application>().applicationContext
                val employees = repository.getAllEmployees()
                val settings = repository.getSettings()
                val officeName = if (_isArabic.value) {
                    settings?.officeNameAr ?: "إدارة حضور المكتب"
                } else {
                    settings?.officeName ?: "Office Attendance Manager"
                }
                val data = employees.map { emp ->
                    emp to repository.getMonthlyRecordsForEmployee(emp.id, yearMonth)
                }
                val file = XlsxExporter.exportOnePageMonthlySheet(
                    context = context,
                    officeName = officeName,
                    employeeRecords = data,
                    yearMonth = yearMonth,
                    isArabic = _isArabic.value
                )
                _lastGeneratedFile.value = file
                _exportMessage.value = file.name
            } catch (e: Exception) {
                _exportMessage.value = "Error: ${e.message}"
            } finally {
                _isGeneratingExcel.value = false
            }
        }
    }

    fun shareReportFiles(context: Context, files: List<File>, specificPackage: String? = null) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        })
        val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(
                Intent.EXTRA_TEXT,
                if (_isArabic.value) "مرفق تقارير الحضور والانصراف (${files.size} ملفات)"
                else "Attached Office Attendance Reports (${files.size} files)"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (specificPackage != null) setPackage(specificPackage)
        }
        val chooser = Intent.createChooser(
            shareIntent,
            if (_isArabic.value) "مشاركة التقارير" else "Share Reports"
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            _exportMessage.value = "Error: ${e.message}"
        }
    }

    fun generateIndividualExcelReport(employeeId: String, yearMonth: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingExcel.value = true
            _exportMessage.value = null
            _generatedFiles.value = emptyList()
            try {
                val context = getApplication<Application>().applicationContext
                val employee = repository.getEmployeeById(employeeId)
                if (employee != null) {
                    val shift = repository.getShiftById(employee.assignedShiftId)
                    val records = repository.getMonthlyRecordsForEmployee(employeeId, yearMonth)
                    val settings = repository.getSettings()
                    val officeName = if (_isArabic.value) {
                        settings?.officeNameAr ?: "إدارة حضور المكتب"
                    } else {
                        settings?.officeName ?: "Office Attendance Manager"
                    }

                    val file = XlsxExporter.exportIndividualReport(
                        context = context,
                        officeName = officeName,
                        employee = employee,
                        shift = shift,
                        records = records,
                        yearMonth = yearMonth,
                        isArabic = _isArabic.value
                    )
                    _lastGeneratedFile.value = file
                    _exportMessage.value = file.name
                }
            } catch (e: Exception) {
                _exportMessage.value = "Error: ${e.message}"
            } finally {
                _isGeneratingExcel.value = false
            }
        }
    }

    fun generateCombinedExcelReport(yearMonth: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingExcel.value = true
            _exportMessage.value = null
            _generatedFiles.value = emptyList()
            try {
                val context = getApplication<Application>().applicationContext
                val employees = repository.getAllEmployees()
                val shifts = repository.getAllShifts().associateBy { it.id }

                val daysInMonth = yearMonth.lengthOfMonth()
                val scheduledWorkDays = (1..daysInMonth).count { day ->
                    val date = yearMonth.atDay(day)
                    date.dayOfWeek.value != 5 && date.dayOfWeek.value != 6
                }

                // Fetch every daily attendance record for each employee using the exact same data source query as individual reports
                val summaries = employees.map { emp ->
                    val empRecords = repository.getMonthlyRecordsForEmployee(emp.id, yearMonth)
                    val daysWorked = empRecords.map { it.attendanceDate }.distinct().size
                    val completedCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime != null }
                    val incompleteCount = empRecords.count { it.checkInTime > 0 && it.checkOutTime == null }
                    val totalHours = empRecords.sumOf { it.totalHours }
                    val totalLate = empRecords.sumOf { it.lateMinutes }
                    val absentDays = maxOf(0, scheduledWorkDays - daysWorked)

                    XlsxExporter.EmployeeReportSummary(
                        employee = emp,
                        assignedShift = shifts[emp.assignedShiftId],
                        records = empRecords,
                        totalDaysWorked = daysWorked,
                        completedDays = completedCount,
                        incompleteRecords = incompleteCount,
                        totalWorkingHours = totalHours,
                        totalLateMinutes = totalLate,
                        absentDays = absentDays
                    )
                }

                val settings = repository.getSettings()
                val officeName = if (_isArabic.value) {
                    settings?.officeNameAr ?: "إدارة حضور المكتب"
                } else {
                    settings?.officeName ?: "Office Attendance Manager"
                }

                val file = XlsxExporter.exportCombinedReport(
                    context = context,
                    officeName = officeName,
                    summaries = summaries,
                    yearMonth = yearMonth,
                    isArabic = _isArabic.value
                )
                _lastGeneratedFile.value = file
                _exportMessage.value = file.name
            } catch (e: Exception) {
                _exportMessage.value = "Error: ${e.message}"
            } finally {
                _isGeneratingExcel.value = false
            }
        }
    }

    fun shareReportFile(context: Context, file: File, specificPackage: String? = null) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(
                    Intent.EXTRA_TEXT,
                    if (_isArabic.value) "مرفق تقرير الحضور والانصراف (${file.name})"
                    else "Attached Office Attendance Report (${file.name})"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (specificPackage != null) {
                    setPackage(specificPackage)
                }
            }

            val chooser = Intent.createChooser(
                shareIntent,
                if (_isArabic.value) "مشاركة التقرير عبر واتساب أو التطبيقات" else "Share Report"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback general chooser
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallback, "Share Report"))
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }
}
