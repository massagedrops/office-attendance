package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Shift
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY id ASC")
    fun getAllShiftsFlow(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts ORDER BY id ASC")
    suspend fun getAllShifts(): List<Shift>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: String): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<Shift>)

    @Update
    suspend fun updateShift(shift: Shift)

    @Delete
    suspend fun deleteShift(shift: Shift)
}
