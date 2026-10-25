package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val employeeName: String,
    val attendanceDate: String, // "yyyy-MM-dd" in Asia/Riyadh
    val assignedShiftId: String,
    val assignedShiftName: String,
    val checkInTime: Long, // epoch millis
    val checkOutTime: Long? = null, // epoch millis
    val checkInFormatted: String, // "HH:mm:ss"
    val checkOutFormatted: String? = null,
    val totalHours: Double = 0.0, // calculated actual hours
    val lateMinutes: Int = 0,
    val status: String, // "CHECKED_IN", "COMPLETED", "INCOMPLETE", "LATE"
    val adminNotes: String? = null,
    val isManualOverride: Boolean = false
)
