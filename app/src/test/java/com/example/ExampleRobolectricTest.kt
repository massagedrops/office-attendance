package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.Localization
import com.example.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Office Attendance Manager", appName)
  }

  @Test
  fun `verify localization translation`() {
    val dashboardAr = Localization.get("dashboard", isArabic = true)
    val dashboardEn = Localization.get("dashboard", isArabic = false)
    assertEquals("لوحة التحكم", dashboardAr)
    assertEquals("Dashboard", dashboardEn)
  }

  @Test
  fun `verify riyadh timezone date format`() {
    val today = TimeUtil.todayDateString()
    assertNotNull(today)
    assertEquals(10, today.length) // yyyy-MM-dd
  }

  @Test
  fun `verify xlsx exporter creates valid openxml file with 9 columns and tick mark`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val employee = com.example.data.model.Employee(
      id = "EMP001",
      name = "Ahmed Al-Mansoor",
      nameAr = "أحمد المنصور",
      qrCode = "EMP001",
      assignedShiftId = "SHIFT_MORNING"
    )
    val shift = com.example.data.model.Shift(
      id = "SHIFT_MORNING",
      nameEn = "Morning Standard Shift",
      nameAr = "وردية الصباح الاعتيادية",
      startTime = "08:00",
      endTime = "16:00"
    )
    val completedRecord = com.example.data.model.AttendanceRecord(
      id = 1,
      employeeId = "EMP001",
      employeeName = "Ahmed Al-Mansoor",
      attendanceDate = "2026-09-01",
      assignedShiftId = "SHIFT_MORNING",
      assignedShiftName = "Morning Standard Shift",
      checkInTime = 1725177600000L,
      checkInFormatted = "08:00:00",
      checkOutTime = 1725206400000L,
      checkOutFormatted = "16:00:00",
      totalHours = 8.0,
      status = "COMPLETED"
    )
    val incompleteRecord = com.example.data.model.AttendanceRecord(
      id = 2,
      employeeId = "EMP001",
      employeeName = "Ahmed Al-Mansoor",
      attendanceDate = "2026-09-02",
      assignedShiftId = "SHIFT_MORNING",
      assignedShiftName = "Morning Standard Shift",
      checkInTime = 1725264000000L,
      checkInFormatted = "08:00:00",
      checkOutTime = null,
      checkOutFormatted = null,
      totalHours = 0.0,
      status = "CHECKED_IN"
    )

    val file = com.example.util.XlsxExporter.exportIndividualReport(
      context = context,
      officeName = "Office Attendance Manager",
      employee = employee,
      shift = shift,
      records = listOf(completedRecord, incompleteRecord),
      yearMonth = java.time.YearMonth.of(2026, 9),
      isArabic = false
    )

    org.junit.Assert.assertTrue(file.exists())
    org.junit.Assert.assertTrue(file.length() > 0)

    // Inspect zip contents
    var hasContentTypes = false
    var hasSharedStrings = false
    var sharedStringsContent = ""

    java.util.zip.ZipFile(file).use { zip ->
      val entries = zip.entries()
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (entry.name == "[Content_Types].xml") hasContentTypes = true
        if (entry.name == "xl/sharedStrings.xml") {
          hasSharedStrings = true
          sharedStringsContent = zip.getInputStream(entry).bufferedReader().readText()
        }
      }
    }

    org.junit.Assert.assertTrue(hasContentTypes)
    org.junit.Assert.assertTrue(hasSharedStrings)

    // Verify 10 mandatory column headers exist
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Date"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Employee ID"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Employee Name"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Assigned Shift"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-in Time"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-out Time"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Total Working Hours"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Late Minutes"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Attendance Status"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-in / Check-out Completion (Tick Mark ✓)"))

    // Verify tick mark and incomplete text exist
    org.junit.Assert.assertTrue(sharedStringsContent.contains("✓"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Incomplete"))
  }

  @Test
  fun `verify combined monthly excel export contains summary plus separate detailed worksheets for all 8 employees`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val employees = (1..8).map { i ->
      val id = "EMP00$i"
      com.example.data.model.Employee(
        id = id,
        name = "Employee $i Name",
        nameAr = "موظف $i تجريبي",
        qrCode = id,
        assignedShiftId = if (i % 2 == 1) "SHIFT_A" else "SHIFT_B"
      )
    }

    val shiftA = com.example.data.model.Shift(
      id = "SHIFT_A",
      nameEn = "Morning Shift",
      nameAr = "وردية الصباح",
      startTime = "08:00",
      endTime = "17:00"
    )
    val shiftB = com.example.data.model.Shift(
      id = "SHIFT_B",
      nameEn = "Evening Shift",
      nameAr = "وردية المساء",
      startTime = "16:00",
      endTime = "01:00"
    )
    val shiftMap = mapOf("SHIFT_A" to shiftA, "SHIFT_B" to shiftB)

    // Build summaries for all 8 employees with completed and incomplete records
    val summaries = employees.map { emp ->
      val shift = shiftMap[emp.assignedShiftId]
      val completedRec = com.example.data.model.AttendanceRecord(
        id = emp.id.filter { it.isDigit() }.toLong() * 10 + 1,
        employeeId = emp.id,
        employeeName = emp.name,
        attendanceDate = "2026-09-01",
        assignedShiftId = emp.assignedShiftId,
        assignedShiftName = shift?.nameEn ?: "Standard",
        checkInTime = 1725177600000L,
        checkInFormatted = "08:00:00 AM",
        checkOutTime = 1725206400000L,
        checkOutFormatted = "04:00:00 PM",
        totalHours = 8.0,
        lateMinutes = 0,
        status = "COMPLETED"
      )
      val incompleteRec = com.example.data.model.AttendanceRecord(
        id = emp.id.filter { it.isDigit() }.toLong() * 10 + 2,
        employeeId = emp.id,
        employeeName = emp.name,
        attendanceDate = "2026-09-02",
        assignedShiftId = emp.assignedShiftId,
        assignedShiftName = shift?.nameEn ?: "Standard",
        checkInTime = 1725267600000L, // 30 min late
        checkInFormatted = "08:30:00 AM",
        checkOutTime = null,
        checkOutFormatted = null,
        totalHours = 0.0,
        lateMinutes = 30,
        status = "CHECKED_IN"
      )

      val records = listOf(completedRec, incompleteRec)
      com.example.util.XlsxExporter.EmployeeReportSummary(
        employee = emp,
        assignedShift = shift,
        records = records,
        totalDaysWorked = 2,
        completedDays = 1,
        incompleteRecords = 1,
        totalWorkingHours = 8.0,
        totalLateMinutes = 30,
        absentDays = 20
      )
    }

    val combinedFile = com.example.util.XlsxExporter.exportCombinedReport(
      context = context,
      officeName = "Office Attendance Manager",
      summaries = summaries,
      yearMonth = java.time.YearMonth.of(2026, 9),
      isArabic = false
    )

    org.junit.Assert.assertTrue(combinedFile.exists())
    org.junit.Assert.assertTrue(combinedFile.length() > 0)

    var workbookXmlContent = ""
    var sharedStringsContent = ""
    val worksheetEntries = mutableListOf<String>()

    java.util.zip.ZipFile(combinedFile).use { zip ->
      val entries = zip.entries()
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (entry.name == "xl/workbook.xml") {
          workbookXmlContent = zip.getInputStream(entry).bufferedReader().readText()
        }
        if (entry.name == "xl/sharedStrings.xml") {
          sharedStringsContent = zip.getInputStream(entry).bufferedReader().readText()
        }
        if (entry.name.startsWith("xl/worksheets/sheet")) {
          worksheetEntries.add(entry.name)
        }
      }
    }

    // 1 Master All Attendance sheet + 1 Summary sheet + 8 Employee sheets = 10 worksheets total
    assertEquals(10, worksheetEntries.size)

    // Verify Master All Attendance sheet and Summary sheet exist
    org.junit.Assert.assertTrue(workbookXmlContent.contains("All Attendance"))
    org.junit.Assert.assertTrue(workbookXmlContent.contains("Summary"))

    // Verify all 8 employee IDs exist in workbook sheets
    for (i in 1..8) {
      val empId = "EMP00$i"
      org.junit.Assert.assertTrue(
        "Workbook should contain sheet for $empId",
        workbookXmlContent.contains(empId)
      )
    }

    // Verify columns exist in shared strings
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Date"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Employee ID"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Employee Name"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Assigned Shift"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-in Time"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-out Time"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Total Working Hours"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Late Minutes"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Attendance Status"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Check-in / Check-out Completion (Tick Mark ✓)"))

    // Verify tick mark for completed record and Incomplete text
    org.junit.Assert.assertTrue(sharedStringsContent.contains("✓"))
    org.junit.Assert.assertTrue(sharedStringsContent.contains("Incomplete"))
  }

  @Test
  fun `verify boss whatsapp check-in message english and arabic`() {
    val employeeName = "Ahmed Al-Mansoor"
    val checkInTime = "08:00 AM"
    val date = "2026-09-22"

    val msgEn = com.example.util.WhatsAppUtil.buildCheckInMessage(
      employeeName = employeeName,
      checkInTime = checkInTime,
      date = date,
      isArabic = false
    )
    val msgAr = com.example.util.WhatsAppUtil.buildCheckInMessage(
      employeeName = employeeName,
      checkInTime = checkInTime,
      date = date,
      isArabic = true
    )

    assertEquals("Ahmed Al-Mansoor coming", msgEn)
    assertEquals("Ahmed Al-Mansoor coming", msgAr)

    // Verify requested format with boms
    val bomsMsg = com.example.util.WhatsAppUtil.buildCheckInMessage(
      employeeName = "boms",
      checkInTime = checkInTime,
      isArabic = false
    )
    assertEquals("boms coming", bomsMsg)
  }

  @Test
  fun `verify boss whatsapp check-out message english and arabic`() {
    val employeeName = "Sara Al-Harbi"
    val checkOutTime = "05:00 PM"
    val date = "2026-09-22"

    val msgEn = com.example.util.WhatsAppUtil.buildCheckOutMessage(
      employeeName = employeeName,
      checkOutTime = checkOutTime,
      date = date,
      isArabic = false
    )
    val msgAr = com.example.util.WhatsAppUtil.buildCheckOutMessage(
      employeeName = employeeName,
      checkOutTime = checkOutTime,
      date = date,
      isArabic = true
    )

    assertEquals("Sara Al-Harbi out", msgEn)
    assertEquals("Sara Al-Harbi out", msgAr)

    // Verify requested format with boms
    val bomsOutMsg = com.example.util.WhatsAppUtil.buildCheckOutMessage(
      employeeName = "boms",
      checkOutTime = checkOutTime,
      isArabic = false
    )
    assertEquals("boms out", bomsOutMsg)
  }

  @Test
  fun `verify whatsapp url generation and phone number cleaning`() {
    val rawPhone = "+966 (50) 123-4567"
    val clean = com.example.util.WhatsAppUtil.cleanPhoneNumber(rawPhone)
    assertEquals("966501234567", clean)

    // Test Saudi local formats starting with 05 and 5
    val localSaudiPhone1 = "0501234567"
    assertEquals("966501234567", com.example.util.WhatsAppUtil.cleanPhoneNumber(localSaudiPhone1))

    val localSaudiPhone2 = "501234567"
    assertEquals("966501234567", com.example.util.WhatsAppUtil.cleanPhoneNumber(localSaudiPhone2))

    val message = "Boss, test arrived"
    val url = com.example.util.WhatsAppUtil.buildWhatsAppUrl(rawPhone, message)
    org.junit.Assert.assertTrue(url.contains("phone=966501234567"))
    org.junit.Assert.assertTrue(url.contains("text=Boss"))
  }

  @Test
  fun `verify boss whatsapp number persistence in app settings`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.example.data.repository.AttendanceRepository(
      employeeDao = db.employeeDao(),
      shiftDao = db.shiftDao(),
      attendanceDao = db.attendanceDao(),
      settingsDao = db.settingsDao(),
      dailyShiftAssignmentDao = db.dailyShiftAssignmentDao()
    )

    // Initial settings
    var settings = repo.getSettings() ?: com.example.data.model.AppSettings()
    assertEquals("", settings.bossWhatsAppNumber)

    // Save boss WhatsApp number
    val updated = settings.copy(bossWhatsAppNumber = "+966501234567")
    repo.saveSettings(updated)

    // Retrieve again to confirm persistence
    val reloaded = repo.getSettings()
    org.junit.Assert.assertNotNull(reloaded)
    assertEquals("+966501234567", reloaded?.bossWhatsAppNumber)

    db.close()
  }
}
