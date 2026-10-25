package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_shift_assignments",
    indices = [Index(value = ["employeeId", "date"], unique = true)]
)
data class DailyShiftAssignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val date: String, // "yyyy-MM-dd"
    val shiftId: String
)
