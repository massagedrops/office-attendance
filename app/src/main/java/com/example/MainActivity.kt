package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AppNavigationBar
import com.example.ui.components.OfficeTopBar
import com.example.ui.screens.AttendanceRecordsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmployeesScreen
import com.example.ui.screens.ExcelExportScreen
import com.example.ui.screens.MonthlyReportsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShiftManagementScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.TimeUtil
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isArabic by viewModel.isArabic.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
            val showAdminAuthDialog by viewModel.showAdminAuthDialog.collectAsState()

            val employees by viewModel.employeesFlow.collectAsState()
            val shifts by viewModel.shiftsFlow.collectAsState()
            val attendanceRecords by viewModel.attendanceRecordsFlow.collectAsState()
            val settings by viewModel.settingsFlow.collectAsState()
            val dashboardStats by viewModel.dashboardStats.collectAsState()
            val scanResult by viewModel.scanResult.collectAsState()
            val isGeneratingExcel by viewModel.isGeneratingExcel.collectAsState()
            val lastGeneratedFile by viewModel.lastGeneratedFile.collectAsState()
            val generatedFiles by viewModel.generatedFiles.collectAsState()
            val exportMessage by viewModel.exportMessage.collectAsState()

            // Real-time ticking clock in Asia/Riyadh
            var currentRiyadhClock by remember {
                mutableStateOf(TimeUtil.formatMillisToTime(System.currentTimeMillis()))
            }
            LaunchedEffect(Unit) {
                while (true) {
                    currentRiyadhClock = TimeUtil.formatMillisToTime(System.currentTimeMillis())
                    delay(1000)
                }
            }

            val officeTitle = if (isArabic) {
                settings?.officeNameAr ?: "إدارة حضور المكتب"
            } else {
                settings?.officeName ?: "Office Attendance Manager"
            }

            // Directional LTR / RTL switching
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            OfficeTopBar(
                                officeName = officeTitle,
                                currentTime = currentRiyadhClock,
                                isArabic = isArabic,
                                isAdminUnlocked = isAdminUnlocked,
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onAdminLockToggle = {
                                    if (isAdminUnlocked) {
                                        viewModel.lockAdmin()
                                    } else {
                                        viewModel.requestAdminAuth {}
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            AppNavigationBar(
                                currentTab = currentTab,
                                isArabic = isArabic,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    stats = dashboardStats,
                                    isArabic = isArabic,
                                    onNavigateToTab = { viewModel.selectTab(it) }
                                )

                                AppTab.QR_SCANNER -> QrScannerScreen(
                                    employees = employees,
                                    scanResult = scanResult,
                                    isArabic = isArabic,
                                    bossWhatsAppNumber = settings?.bossWhatsAppNumber ?: "",
                                    onScanCode = { viewModel.processScannedQr(it) },
                                    onConfirmAttendance = { pending -> viewModel.confirmAttendance(pending) },
                                    onDismissScanResult = { viewModel.dismissScanResult() },
                                    onRequestAdminOverride = {
                                        viewModel.requestAdminAuth {
                                             viewModel.selectTab(AppTab.ATTENDANCE_RECORDS)
                                        }
                                    },
                                    onNotifyBossWhatsApp = { empName, time, date, isCheckIn ->
                                        viewModel.notifyBossOnWhatsApp(context, empName, time, date, isCheckIn)
                                    }
                                )

                                AppTab.EMPLOYEES -> EmployeesScreen(
                                    employees = employees,
                                    shifts = shifts,
                                    records = attendanceRecords,
                                    isArabic = isArabic,
                                    isAdminUnlocked = isAdminUnlocked,
                                    onRequestAdminAuth = { action -> viewModel.requestAdminAuth(action) },
                                    onSaveEmployee = { viewModel.saveEmployee(it) },
                                    onDeleteEmployee = { viewModel.deleteEmployee(it) },
                                    onRegenerateQr = { viewModel.regenerateEmployeeQr(it) }
                                )

                                AppTab.SHIFTS -> ShiftManagementScreen(
                                    shifts = shifts,
                                    employees = employees,
                                    isArabic = isArabic,
                                    onRequestAdminAuth = { action -> viewModel.requestAdminAuth(action) },
                                    onSaveShift = { viewModel.saveShift(it) },
                                    onDeleteShift = { viewModel.deleteShift(it) },
                                    onAssignDailyShift = { empId, date, shiftId ->
                                        viewModel.assignDailyShiftOverride(empId, date, shiftId)
                                    }
                                )

                                AppTab.ATTENDANCE_RECORDS -> AttendanceRecordsScreen(
                                    records = attendanceRecords,
                                    employees = employees,
                                    isArabic = isArabic,
                                    onRequestAdminAuth = { action -> viewModel.requestAdminAuth(action) },
                                    onSaveCorrection = { viewModel.saveAttendanceCorrection(it) },
                                    onDeleteRecord = { viewModel.deleteAttendanceRecord(it) }
                                )

                                AppTab.MONTHLY_REPORTS -> MonthlyReportsScreen(
                                    employees = employees,
                                    shifts = shifts,
                                    allRecords = attendanceRecords,
                                    isArabic = isArabic,
                                    onNavigateToTab = { viewModel.selectTab(it) },
                                    onExportIndividual = { empId, ym ->
                                        viewModel.generateIndividualExcelReport(empId, ym)
                                    },
                                    onExportCombined = { ym ->
                                        viewModel.generateCombinedExcelReport(ym)
                                    }
                                )

                                AppTab.EXCEL_EXPORT -> ExcelExportScreen(
                                    employees = employees,
                                    isArabic = isArabic,
                                    isGeneratingExcel = isGeneratingExcel,
                                    lastGeneratedFile = lastGeneratedFile,
                                    generatedFiles = generatedFiles,
                                    exportMessage = exportMessage,
                                    onGenerateIndividual = { empId, ym ->
                                        viewModel.generateIndividualExcelReport(empId, ym)
                                    },
                                    onGenerateCombined = { ym ->
                                        viewModel.generateCombinedExcelReport(ym)
                                    },
                                    onGenerateAllSeparate = { ym ->
                                        viewModel.generateAllIndividualExcelReports(ym)
                                    },
                                    onGenerateOnePage = { ym ->
                                        viewModel.generateOnePageMonthlySheet(ym)
                                    },
                                    onShareFile = { ctx, file, pkg ->
                                        viewModel.shareReportFile(ctx, file, pkg)
                                    },
                                    onShareFiles = { ctx, files, pkg ->
                                        viewModel.shareReportFiles(ctx, files, pkg)
                                    }
                                )

                                AppTab.SETTINGS -> SettingsScreen(
                                    settings = settings,
                                    isArabic = isArabic,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onRequestAdminAuth = { action -> viewModel.requestAdminAuth(action) },
                                    onSaveSettings = { name, pin, grace, bossNumber ->
                                        viewModel.updateOfficeSettings(name, pin, grace, bossNumber)
                                    },
                                    onSaveBossWhatsApp = { bossNumber ->
                                        viewModel.updateBossWhatsAppNumber(bossNumber)
                                    }
                                )
                            }
                        }

                        // Admin Unlock Security PIN Modal
                        if (showAdminAuthDialog) {
                            AdminLoginDialog(
                                isArabic = isArabic,
                                onDismiss = { viewModel.dismissAdminAuthDialog() },
                                onVerifyPin = { pin -> viewModel.verifyAdminPin(pin) }
                            )
                        }
                    }
                }
            }
        }
    }
}
