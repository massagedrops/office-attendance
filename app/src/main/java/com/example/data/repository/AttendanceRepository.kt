package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.DailyShiftAssignmentDao
import com.example.data.local.EmployeeDao
import com.example.data.local.SettingsDao
import com.example.data.local.ShiftDao
import com.example.data.model.AppSettings
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyShiftAssignment
import com.example.data.model.Employee
import com.example.data.model.Shift
import com.example.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

sealed class ScanResult {
    data class PendingConfirmation(
        val employee: Employee,
        val isCheckIn: Boolean,
        val date: String,
        val timeFormatted: String,
        val timeMillis: Long,
        val shiftName: String,
        val openRecord: AttendanceRecord? = null,
        val lateMinutes: Int = 0
    ) : ScanResult()

    data class SuccessCheckIn(
        val employee: Employee,
        val record: AttendanceRecord,
        val lateMinutes: Int,
        val shiftName: String
    ) : ScanResult()

    data class SuccessCheckOut(
        val employee: Employee,
        val record: AttendanceRecord,
        val totalHours: Double,
        val shiftName: String
    ) : ScanResult()

    data class DuplicateWarning(val employeeName: String, val message: String) : ScanResult()
    data class InactiveEmployee(val employeeName: String) : ScanResult()
    data class NotFound(val scannedCode: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

class AttendanceRepository(
    private val employeeDao: EmployeeDao,
    private val shiftDao: ShiftDao,
    private val attendanceDao: AttendanceDao,
    private val settingsDao: SettingsDao,
    private val dailyShiftAssignmentDao: DailyShiftAssignmentDao
) {
    // Last confirmed attendance tracking to prevent accidental rapid repeat scans
    private var lastConfirmedEmployeeId: String? = null
    private var lastConfirmedTimestamp: Long = 0

    fun resetScanLock() {
        lastConfirmedEmployeeId = null
        lastConfirmedTimestamp = 0L
    }

    // Flows
    val allEmployeesFlow: Flow<List<Employee>> = employeeDao.getAllEmployeesFlow()
    val allShiftsFlow: Flow<List<Shift>> = shiftDao.getAllShiftsFlow()
    val allRecordsFlow: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecordsFlow()
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()
    val activeCheckedInFlow: Flow<List<AttendanceRecord>> = attendanceDao.getActiveCheckedInRecordsFlow()

    fun getRecordsForDateFlow(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsForDateFlow(date)

    // Employee operations
    suspend fun getAllEmployees(): List<Employee> = employeeDao.getAllEmployees()
    suspend fun getEmployeeById(id: String): Employee? = employeeDao.getEmployeeById(id)
    suspend fun insertEmployee(employee: Employee) = employeeDao.insertEmployee(employee)
    suspend fun updateEmployee(employee: Employee) = employeeDao.updateEmployee(employee)
    suspend fun deleteEmployee(employee: Employee) = employeeDao.deleteEmployee(employee)

    suspend fun regenerateEmployeeQr(employeeId: String): String? {
        val emp = employeeDao.getEmployeeById(employeeId) ?: return null
        val newQr = "EMP-${emp.id}-${System.currentTimeMillis() % 10000}"
        val updated = emp.copy(qrCode = newQr)
        employeeDao.updateEmployee(updated)
        return newQr
    }

    // Shift operations
    suspend fun getAllShifts(): List<Shift> = shiftDao.getAllShifts()
    suspend fun getShiftById(id: String): Shift? = shiftDao.getShiftById(id)
    suspend fun insertShift(shift: Shift) = shiftDao.insertShift(shift)
    suspend fun updateShift(shift: Shift) = shiftDao.updateShift(shift)
    suspend fun deleteShift(shift: Shift) = shiftDao.deleteShift(shift)

    // Daily Shift Assignment
    suspend fun setDailyShiftOverride(employeeId: String, date: String, shiftId: String) {
        dailyShiftAssignmentDao.setAssignment(
            DailyShiftAssignment(employeeId = employeeId, date = date, shiftId = shiftId)
        )
    }

    suspend fun getEffectiveShiftForEmployee(employeeId: String, date: String): Shift? {
        val override = dailyShiftAssignmentDao.getAssignment(employeeId, date)
        if (override != null) {
            val shift = shiftDao.getShiftById(override.shiftId)
            if (shift != null) return shift
        }
        val emp = employeeDao.getEmployeeById(employeeId) ?: return null
        return shiftDao.getShiftById(emp.assignedShiftId)
    }

    // Attendance Scanning Engine
    suspend fun processQrScan(scannedCode: String): ScanResult {
        val cleanCode = scannedCode.trim()
        val nowMillis = System.currentTimeMillis()

        // Find employee by qrCode or by direct Employee ID
        val employee = employeeDao.getEmployeeByQrCode(cleanCode)
            ?: employeeDao.getEmployeeById(cleanCode)
            ?: return ScanResult.NotFound(cleanCode)

        if (!employee.isActive) {
            return ScanResult.InactiveEmployee(employee.name)
        }

        val todayDate = TimeUtil.todayDateString()
        val formattedTime = TimeUtil.formatMillisToTime(nowMillis)
        val shift = getEffectiveShiftForEmployee(employee.id, todayDate)
        val shiftName = shift?.nameEn ?: employee.assignedShiftId

        // Check if there is an active open check-in (within the last 24 hours)
        val latestRecord = attendanceDao.getLatestRecordForEmployee(employee.id)
        val hasActiveCheckIn = latestRecord != null && 
                latestRecord.checkOutTime == null && 
                (nowMillis - latestRecord.checkInTime) < (24 * 3600 * 1000L)

        if (hasActiveCheckIn && latestRecord != null) {
            // Employee is currently checked in -> Check-out (OUT) pending confirmation!
            return ScanResult.PendingConfirmation(
                employee = employee,
                isCheckIn = false,
                date = todayDate,
                timeFormatted = formattedTime,
                timeMillis = nowMillis,
                shiftName = shiftName,
                openRecord = latestRecord
            )
        } else {
            // Employee does not have an open check-in -> Check-in (IN) pending confirmation!
            val settings = settingsDao.getSettings()
            val graceMinutes = shift?.graceMinutes ?: settings?.lateGraceMinutes ?: 15
            val lateMins = if (shift != null) {
                TimeUtil.calculateLateMinutes(nowMillis, shift.startTime, graceMinutes, todayDate)
            } else 0

            return ScanResult.PendingConfirmation(
                employee = employee,
                isCheckIn = true,
                date = todayDate,
                timeFormatted = formattedTime,
                timeMillis = nowMillis,
                shiftName = shiftName,
                lateMinutes = lateMins
            )
        }
    }

    suspend fun confirmPendingAttendance(pending: ScanResult.PendingConfirmation): ScanResult {
        val nowMillis = System.currentTimeMillis()
        val formattedTime = TimeUtil.formatMillisToTime(nowMillis)

        // Record last confirmed employee & timestamp
        lastConfirmedEmployeeId = pending.employee.id
        lastConfirmedTimestamp = nowMillis

        val todayDate = pending.date

        if (pending.isCheckIn) {
            val settings = settingsDao.getSettings()
            val shift = getEffectiveShiftForEmployee(pending.employee.id, todayDate)
            val graceMinutes = shift?.graceMinutes ?: settings?.lateGraceMinutes ?: 15
            val lateMins = if (shift != null) {
                TimeUtil.calculateLateMinutes(nowMillis, shift.startTime, graceMinutes, todayDate)
            } else pending.lateMinutes

            val initialStatus = if (lateMins > 0) "LATE" else "CHECKED_IN"

            val newRecord = AttendanceRecord(
                employeeId = pending.employee.id,
                employeeName = pending.employee.name,
                attendanceDate = todayDate,
                assignedShiftId = shift?.id ?: pending.employee.assignedShiftId,
                assignedShiftName = pending.shiftName,
                checkInTime = nowMillis,
                checkInFormatted = formattedTime,
                lateMinutes = lateMins,
                status = initialStatus
            )
            val id = attendanceDao.insertRecord(newRecord)
            val savedRecord = newRecord.copy(id = id)

            return ScanResult.SuccessCheckIn(
                employee = pending.employee,
                record = savedRecord,
                lateMinutes = lateMins,
                shiftName = pending.shiftName
            )
        } else {
            val recordToClose = pending.openRecord
                ?: attendanceDao.getLatestRecordForEmployee(pending.employee.id)

            if (recordToClose != null && recordToClose.checkOutTime == null) {
                val actualHours = TimeUtil.calculateWorkingHours(recordToClose.checkInTime, nowMillis)
                val updatedRecord = recordToClose.copy(
                    checkOutTime = nowMillis,
                    checkOutFormatted = formattedTime,
                    totalHours = actualHours,
                    status = if (recordToClose.lateMinutes > 0) "LATE_COMPLETED" else "COMPLETED"
                )
                attendanceDao.updateRecord(updatedRecord)

                return ScanResult.SuccessCheckOut(
                    employee = pending.employee,
                    record = updatedRecord,
                    totalHours = actualHours,
                    shiftName = pending.shiftName
                )
            } else {
                // If there was no open check-in, record departure directly
                val shift = getEffectiveShiftForEmployee(pending.employee.id, todayDate)
                val nominalStart = nowMillis - (8 * 3600 * 1000L)
                val directOutRecord = AttendanceRecord(
                    employeeId = pending.employee.id,
                    employeeName = pending.employee.name,
                    attendanceDate = todayDate,
                    assignedShiftId = shift?.id ?: pending.employee.assignedShiftId,
                    assignedShiftName = pending.shiftName,
                    checkInTime = nominalStart,
                    checkInFormatted = TimeUtil.formatMillisToTime(nominalStart),
                    checkOutTime = nowMillis,
                    checkOutFormatted = formattedTime,
                    totalHours = 8.0,
                    status = "COMPLETED"
                )
                val id = attendanceDao.insertRecord(directOutRecord)
                return ScanResult.SuccessCheckOut(
                    employee = pending.employee,
                    record = directOutRecord.copy(id = id),
                    totalHours = 8.0,
                    shiftName = pending.shiftName
                )
            }
        }
    }

    // Attendance record management
    suspend fun updateAttendanceRecord(record: AttendanceRecord) = attendanceDao.updateRecord(record)
    suspend fun deleteAttendanceRecord(id: Long) = attendanceDao.deleteRecordById(id)

    suspend fun getMonthlyRecordsForEmployee(employeeId: String, yearMonth: YearMonth): List<AttendanceRecord> {
        val monthPrefix = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}%"
        return attendanceDao.getMonthlyRecordsForEmployee(employeeId, monthPrefix)
    }

    suspend fun getAllMonthlyRecords(yearMonth: YearMonth): List<AttendanceRecord> {
        val monthPrefix = "${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}%"
        return attendanceDao.getAllMonthlyRecords(monthPrefix)
    }

    // Settings
    suspend fun getSettings(): AppSettings? = settingsDao.getSettings()
    suspend fun saveSettings(settings: AppSettings) = settingsDao.saveSettings(settings)
}
