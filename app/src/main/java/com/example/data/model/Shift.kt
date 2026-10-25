package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey
    val id: String, // e.g. "SHIFT_A", "SHIFT_B"
    val nameEn: String,
    val nameAr: String,
    val startTime: String, // "14:00" in 24h
    val endTime: String,   // "02:00" in 24h (overnight)
    val graceMinutes: Int = 15,
    val colorHex: String = "#1E88E5"
)
