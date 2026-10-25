package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettings
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyShiftAssignment
import com.example.data.model.Employee
import com.example.data.model.Shift
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Employee::class,
        Shift::class,
        DailyShiftAssignment::class,
        AttendanceRecord::class,
        AppSettings::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun shiftDao(): ShiftDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun settingsDao(): SettingsDao
    abstract fun dailyShiftAssignmentDao(): DailyShiftAssignmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN bossWhatsAppNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employees ADD COLUMN designation TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "office_attendance.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial data asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val shifts = listOf(
                Shift(
                    id = "SHIFT_A",
                    nameEn = "Shift A (02:00 PM - 02:00 AM)",
                    nameAr = "وردية أ (02:00 م - 02:00 ص)",
                    startTime = "14:00",
                    endTime = "02:00",
                    graceMinutes = 15,
                    colorHex = "#2563EB"
                ),
                Shift(
                    id = "SHIFT_B",
                    nameEn = "Shift B (12:00 PM - 12:00 AM)",
                    nameAr = "وردية ب (12:00 م - 12:00 ص)",
                    startTime = "12:00",
                    endTime = "00:00",
                    graceMinutes = 15,
                    colorHex = "#0D9488"
                ),
                Shift(
                    id = "SHIFT_MORNING",
                    nameEn = "Morning Shift (08:00 AM - 05:00 PM)",
                    nameAr = "وردية الصباح (08:00 ص - 05:00 م)",
                    startTime = "08:00",
                    endTime = "17:00",
                    graceMinutes = 15,
                    colorHex = "#D97706"
                )
            )
            database.shiftDao().insertAll(shifts)

            val employees = listOf(
                Employee(
                    id = "EMP001",
                    name = "Mohammed Al-Otaibi",
                    nameAr = "محمد العتيبي",
                    phone = "+966 50 123 4567",
                    qrCode = "EMP001",
                    assignedShiftId = "SHIFT_A",
                    isActive = true
                ),
                Employee(
                    id = "EMP002",
                    name = "Sarah Al-Ghamdi",
                    nameAr = "سارة الغامدي",
                    phone = "+966 50 234 5678",
                    qrCode = "EMP002",
                    assignedShiftId = "SHIFT_A",
                    isActive = true
                ),
                Employee(
                    id = "EMP003",
                    name = "Abdullah Al-Shehri",
                    nameAr = "عبدالله الشهري",
                    phone = "+966 50 345 6789",
                    qrCode = "EMP003",
                    assignedShiftId = "SHIFT_B",
                    isActive = true
                ),
                Employee(
                    id = "EMP004",
                    name = "Fatima Al-Zahrani",
                    nameAr = "فاطمة الزهراني",
                    phone = "+966 50 456 7890",
                    qrCode = "EMP004",
                    assignedShiftId = "SHIFT_B",
                    isActive = true
                ),
                Employee(
                    id = "EMP005",
                    name = "Omar Al-Harbi",
                    nameAr = "عمر الحربي",
                    phone = "+966 50 567 8901",
                    qrCode = "EMP005",
                    assignedShiftId = "SHIFT_A",
                    isActive = true
                ),
                Employee(
                    id = "EMP006",
                    name = "Reem Al-Dosari",
                    nameAr = "ريم الدوسري",
                    phone = "+966 50 678 9012",
                    qrCode = "EMP006",
                    assignedShiftId = "SHIFT_B",
                    isActive = true
                ),
                Employee(
                    id = "EMP007",
                    name = "Khalid Al-Mutairi",
                    nameAr = "خالد المطيري",
                    phone = "+966 50 789 0123",
                    qrCode = "EMP007",
                    assignedShiftId = "SHIFT_A",
                    isActive = true
                ),
                Employee(
                    id = "EMP008",
                    name = "Noura Al-Qahtani",
                    nameAr = "نورة القحطاني",
                    phone = "+966 50 890 1234",
                    qrCode = "EMP008",
                    assignedShiftId = "SHIFT_B",
                    isActive = true
                )
            )
            database.employeeDao().insertAll(employees)

            val defaultSettings = AppSettings(
                key = "singleton_settings",
                officeName = "Office Attendance Manager",
                officeNameAr = "إدارة حضور المكتب",
                adminPin = "1234",
                language = "ar",
                timezone = "Asia/Riyadh",
                lateGraceMinutes = 15,
                workingDaysMask = 31
            )
            database.settingsDao().saveSettings(defaultSettings)
        }
    }
}
