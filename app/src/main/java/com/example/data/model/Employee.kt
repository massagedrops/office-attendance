package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey
    val id: String, // e.g. EMP001
    val name: String,
    val nameAr: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val designation: String = "", // e.g. Receptionist (shown on ID card)
    val qrCode: String, // unique QR string
    val assignedShiftId: String, // default shift id
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
