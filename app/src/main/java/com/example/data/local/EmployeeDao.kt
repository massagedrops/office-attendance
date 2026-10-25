package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY id ASC")
    fun getAllEmployeesFlow(): Flow<List<Employee>>

    @Query("SELECT * FROM employees ORDER BY id ASC")
    suspend fun getAllEmployees(): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): Employee?

    @Query("SELECT * FROM employees WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getEmployeeByQrCode(qrCode: String): Employee?

    @Query("SELECT COUNT(*) FROM employees WHERE isActive = 1")
    fun getActiveEmployeeCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: String)
}
