package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyShiftAssignment
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyShiftAssignmentDao {
    @Query("SELECT * FROM daily_shift_assignments WHERE date = :date")
    fun getAssignmentsForDateFlow(date: String): Flow<List<DailyShiftAssignment>>

    @Query("SELECT * FROM daily_shift_assignments WHERE employeeId = :empId AND date = :date LIMIT 1")
    suspend fun getAssignment(empId: String, date: String): DailyShiftAssignment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAssignment(assignment: DailyShiftAssignment)

    @Query("DELETE FROM daily_shift_assignments WHERE employeeId = :empId AND date = :date")
    suspend fun removeAssignment(empId: String, date: String)
}
