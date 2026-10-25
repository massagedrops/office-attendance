package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val key: String = "singleton_settings",
    val officeName: String = "Office Attendance Manager",
    val officeNameAr: String = "إدارة حضور المكتب",
    val adminPin: String = "1234",
    val language: String = "ar", // "ar" or "en"
    val timezone: String = "Asia/Riyadh",
    val lateGraceMinutes: Int = 15,
    val workingDaysMask: Int = 31, // Bitmask or Sunday to Thursday
    val bossWhatsAppNumber: String = ""
)
