package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY checkInTime DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY checkInTime DESC")
    suspend fun getAllRecords(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE attendanceDate = :date ORDER BY checkInTime DESC")
    fun getRecordsForDateFlow(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE attendanceDate = :date ORDER BY checkInTime DESC")
    suspend fun getRecordsForDate(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId AND attendanceDate = :date ORDER BY checkInTime DESC LIMIT 1")
    suspend fun getLatestRecordForEmployeeAndDate(empId: String, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId ORDER BY checkInTime DESC LIMIT 1")
    suspend fun getLatestRecordForEmployee(empId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId AND attendanceDate LIKE :monthPrefix ORDER BY attendanceDate ASC, checkInTime ASC")
    suspend fun getMonthlyRecordsForEmployee(empId: String, monthPrefix: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE attendanceDate LIKE :monthPrefix ORDER BY attendanceDate ASC, checkInTime ASC")
    suspend fun getAllMonthlyRecords(monthPrefix: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE checkOutTime IS NULL ORDER BY checkInTime DESC")
    fun getActiveCheckedInRecordsFlow(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
