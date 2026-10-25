package com.example.util

object Localization {

    enum class Language(val code: String, val title: String) {
        ARABIC("ar", "العربية"),
        ENGLISH("en", "English")
    }

    private val translations = mapOf(
        // General & Navigation
        "app_title" to Pair("Office Attendance Manager", "إدارة حضور المكتب"),
        "dashboard" to Pair("Dashboard", "لوحة التحكم"),
        "qr_scanner" to Pair("QR Scanner", "ماسح الباركود"),
        "employees" to Pair("Employees", "الموظفين"),
        "shifts" to Pair("Shift Management", "إدارة الورديات"),
        "records" to Pair("Attendance Records", "سجلات الحضور"),
        "reports" to Pair("Monthly Reports", "التقارير الشهرية"),
        "export" to Pair("Excel Export", "تصدير إكسيل"),
        "settings" to Pair("Settings", "الإعدادات"),

        // Dashboard Stats
        "total_employees" to Pair("Total Employees", "إجمالي الموظفين"),
        "checked_in_now" to Pair("Checked In Now", "المتواجدون حالياً"),
        "checked_out_now" to Pair("Checked Out", "المنصرفون"),
        "today_attendance" to Pair("Today's Attendance", "حضور اليوم"),
        "late_arrivals" to Pair("Late Arrivals", "المتأخرون"),
        "absent_today" to Pair("Absent Today", "الغياب اليوم"),
        "total_hours_today" to Pair("Total Working Hours", "إجمالي ساعات العمل"),
        "quick_scan" to Pair("Open QR Scanner", "فتح ماسح الباركود"),
        "recent_activity" to Pair("Recent Activity", "النشاط الأخير"),
        "no_activity_today" to Pair("No attendance activity recorded yet today", "لم يتم تسجيل أي حضور اليوم بعد"),

        // QR Attendance Workflow
        "scan_instruction" to Pair("Scan Employee QR Code for IN or OUT", "امسح باركود الموظف لتسجيل الدخول (IN) أو الخروج (OUT)"),
        "confirm_attendance" to Pair("Confirm Attendance", "تأكيد تسجيل الحضور"),
        "title_confirm_in" to Pair("Confirm IN (Check-in)", "تأكيد تسجيل الحضور (Confirm IN)"),
        "title_confirm_out" to Pair("Confirm OUT (Check-out)", "تأكيد تسجيل الانصراف (Confirm OUT)"),
        "confirm_check_in" to Pair("Confirm IN (Check-in)", "تأكيد تسجيل الدخول (Confirm IN)"),
        "confirm_check_out" to Pair("Confirm OUT (Check-out)", "تأكيد تسجيل الخروج (Confirm OUT)"),
        "check_in_confirmed_success" to Pair("Checked IN Successfully!", "تم تسجيل الدخول بنجاح (IN)!"),
        "check_out_confirmed_success" to Pair("Checked OUT Successfully!", "تم تسجيل الخروج بنجاح (OUT)!"),
        "check_in_success" to Pair("Checked IN Successfully!", "تم تسجيل الدخول بنجاح (IN)!"),
        "check_out_success" to Pair("Checked OUT Successfully!", "تم تسجيل الخروج بنجاح (OUT)!"),
        "duplicate_scan_warning" to Pair("Already processed. Please check the attendance status.", "تمت المعالجة بالفعل. يرجى التحقق من حالة الحضور."),
        "attendance_action" to Pair("Attendance Action", "نوع العملية"),
        "action_check_in" to Pair("IN (Check-in)", "IN - تسجيل حضور (دخول)"),
        "action_check_out" to Pair("OUT (Check-out)", "OUT - تسجيل انصراف (خروج)"),
        "badge_in" to Pair("IN (Check-in)", "دخول (IN)"),
        "badge_out" to Pair("OUT (Check-out)", "خروج (OUT)"),
        "select_action_hint" to Pair("Action mode:", "نوع العملية:"),
        "date_and_time" to Pair("Date & Time", "التاريخ والوقت"),
        "done" to Pair("Done", "تم"),
        "cancel" to Pair("Cancel", "إلغاء"),
        "employee_not_found" to Pair("Unrecognized QR Code. Employee not found!", "رمز QR غير معروف. لم يتم العثور على الموظف!"),
        "employee_inactive_warning" to Pair("This employee account is currently inactive!", "حساب هذا الموظف غير نشط حالياً!"),
        "camera_permission_required" to Pair("Camera permission is required to scan QR codes", "إذن الكاميرا مطلوب لمسح رموز QR"),
        "grant_permission" to Pair("Grant Permission", "منح الإذن"),
        "simulate_test_scan" to Pair("Quick Select / Test Scan", "اختيار سريع / فحص تجريبي"),
        "select_employee_to_scan" to Pair("Select employee to simulate scan:", "اختر موظفاً لتسجيل الحضور فوراً:"),

        // Employee Management
        "add_employee" to Pair("Add Employee", "إضافة موظف"),
        "edit_employee" to Pair("Edit Employee", "تعديل الموظف"),
        "delete_employee" to Pair("Delete Employee", "حذف الموظف"),
        "employee_id" to Pair("Employee ID", "الرقم الوظيفي"),
        "full_name_en" to Pair("Full Name (English)", "الاسم الكامل (إنجليزي)"),
        "full_name_ar" to Pair("Full Name (Arabic)", "الاسم الكامل (عربي)"),
        "phone_number" to Pair("Phone Number (Optional)", "رقم الهاتف (اختياري)"),
        "assigned_shift" to Pair("Assigned Shift", "الوردية المخصصة"),
        "status" to Pair("Status", "الحالة"),
        "active" to Pair("Active", "نشط"),
        "inactive" to Pair("Inactive", "غير نشط"),
        "view_qr" to Pair("View / Share QR", "عرض / مشاركة الرمز"),
        "regenerate_qr" to Pair("Regenerate QR Code", "إعادة إنشاء رمز QR"),
        "qr_code_for" to Pair("QR Code for", "رمز QR للموظف:"),
        "print_save_qr" to Pair("Print or Save QR Code for employee identification", "اطبع أو احفظ رمز QR لتعريف الموظف"),

        // Shifts
        "add_shift" to Pair("Add New Shift", "إضافة وردية جديدة"),
        "edit_shift" to Pair("Edit Shift", "تعديل الوردية"),
        "shift_name_en" to Pair("Shift Name (English)", "اسم الوردية (إنجليزي)"),
        "shift_name_ar" to Pair("Shift Name (Arabic)", "اسم الوردية (عربي)"),
        "start_time" to Pair("Start Time (24h e.g. 14:00)", "وقت البدء (24 ساعة مثل 14:00)"),
        "end_time" to Pair("End Time (24h e.g. 02:00)", "وقت الانتهاء (24 ساعة مثل 02:00)"),
        "grace_period_mins" to Pair("Grace Period (Minutes)", "فترة السماح (بالدقائق)"),
        "overnight_notice" to Pair("Overnight shift (crosses midnight into next day)", "وردية ليلية (تمتد بعد منتصف الليل لليوم التالي)"),
        "assign_daily_shift" to Pair("Assign Daily Shift Override", "تخصيص وردية ليوم محدد"),
        "select_date" to Pair("Select Date", "اختر التاريخ"),

        // Attendance Records
        "filter_by_employee" to Pair("All Employees", "جميع الموظفين"),
        "filter_by_status" to Pair("All Statuses", "جميع الحالات"),
        "search_hint" to Pair("Search by name or employee ID...", "البحث بالاسم أو الرقم الوظيفي..."),
        "check_in" to Pair("Check-In", "تسجيل الحضور"),
        "check_out" to Pair("Check-Out", "تسجيل الانصراف"),
        "working_hours" to Pair("Working Hours", "ساعات العمل"),
        "late" to Pair("Late", "متأخر"),
        "ontime" to Pair("On Time", "في الموعد"),
        "incomplete" to Pair("Incomplete (Missing Check-out)", "غير مكتمل (لم ينصرف)"),
        "in_progress" to Pair("In Progress", "قيد العمل"),
        "completed" to Pair("Completed", "مكتمل"),
        "admin_correction" to Pair("Admin Override / Correction", "تعديل إداري / تصحيح"),
        "correction_notes" to Pair("Admin Notes", "ملاحظات الإدارة"),
        "save_correction" to Pair("Save Correction", "حفظ التصحيح"),

        // Excel & Reports
        "select_month_year" to Pair("Select Month & Year", "اختر الشهر والسنة"),
        "export_individual" to Pair("Individual Employee Report", "تقرير موظف فردي"),
        "export_combined" to Pair("Combined Report (All Employees)", "التقرير الشامل (جميع الموظفين)"),
        "generate_excel" to Pair("Generate Excel File (.xlsx)", "إنشاء ملف إكسيل (.xlsx)"),
        "share_whatsapp" to Pair("Share via WhatsApp", "مشاركة عبر واتساب"),
        "download_file" to Pair("Download Excel File", "تحميل ملف الإكسيل"),
        "report_ready" to Pair("Excel report generated successfully!", "تم إنشاء تقرير الإكسيل بنجاح!"),
        "select_employee_report" to Pair("Select Employee", "اختر الموظف"),
        "monthly_summary" to Pair("Monthly Summary", "الملخص الشهري"),
        "total_days_worked" to Pair("Total Days Worked", "أيام الحضور"),
        "absent_days" to Pair("Absent Days", "أيام الغياب"),
        "total_late_minutes" to Pair("Total Late Minutes", "إجمالي دقائق التأخير"),

        // Admin Auth
        "admin_security" to Pair("Admin Security", "أمان الإدارة"),
        "enter_pin" to Pair("Enter Admin PIN to unlock management actions", "أدخل رمز PIN للإدارة لفتح العمليات الإدارية"),
        "default_pin_hint" to Pair("Default PIN is 1234", "رمز PIN الافتراضي هو 1234"),
        "pin_incorrect" to Pair("Incorrect PIN! Please try again.", "رمز PIN غير صحيح! حاول مجدداً."),
        "unlocked" to Pair("Admin Unlocked", "تم فتح صلاحيات الإدارة"),
        "lock_admin" to Pair("Lock Admin Mode", "قفل وضع الإدارة"),
        "unlock" to Pair("Unlock", "فتح"),
        "cancel" to Pair("Cancel", "إلغاء"),
        "confirm" to Pair("Confirm", "تأكيد"),
        "delete_confirm" to Pair("Are you sure you want to delete this? This action cannot be undone.", "هل أنت متأكد من الحذف؟ لا يمكن التراجع عن هذا الإجراء."),
        "save" to Pair("Save", "حفظ"),
        "close" to Pair("Close", "إغلاق"),

        // Settings
        "office_name" to Pair("Office Name", "اسم المكتب"),
        "change_admin_pin" to Pair("Change Admin PIN", "تغيير رمز PIN للإدارة"),
        "current_pin" to Pair("Current PIN", "الرمز الحالي"),
        "new_pin" to Pair("New 4-Digit PIN", "الرمز الجديد (4 أرقام)"),
        "timezone_label" to Pair("Timezone (Saudi Arabia)", "المنطقة الزمنية (المملكة العربية السعودية)"),
        "language_switcher" to Pair("App Language / لغة التطبيق", "لغة التطبيق / App Language"),
        "data_backup" to Pair("Database Backup & Restore", "النسخ الاحتياطي واستعادة البيانات"),
        "backup_json" to Pair("Export Database Backup (JSON)", "تصدير نسخة احتياطية (JSON)"),
        "restore_json" to Pair("Restore from Backup (JSON)", "استعادة من نسخة احتياطية (JSON)"),
        "settings_saved" to Pair("Settings saved successfully!", "تم حفظ الإعدادات بنجاح!"),

        // Boss WhatsApp Notifications
        "boss_whatsapp_settings" to Pair("Boss WhatsApp Settings", "إعدادات واتساب المدير"),
        "boss_whatsapp_desc" to Pair(
            "Configure the Boss WhatsApp phone number with Saudi Arabia country code (+966). This number is saved and reused automatically whenever any employee scans check-in or check-out.",
            "تهيئة رقم واتساب المدير برمز دولة المملكة العربية السعودية (+966). يتم حفظ هذا الرقم واستخدامه تلقائياً عند مسح حضور أو انصراف أي موظف."
        ),
        "notify_boss_whatsapp" to Pair("Notify Boss on WhatsApp", "إشعار المدير عبر واتساب"),
        "boss_whatsapp_number" to Pair("Boss WhatsApp Number", "رقم واتساب المدير"),
        "boss_whatsapp_help" to Pair(
            "Enter Saudi Arabia mobile number with +966 (e.g., +966501234567 or 0501234567). It will be saved permanently.",
            "أدخل رقم الجوال السعودي مع رمز +966 (مثال: 966501234567+ أو 0501234567). سيتم حفظ الرقم بشكل دائم."
        ),
        "save_boss_whatsapp" to Pair("Save WhatsApp Number", "حفظ رقم الواتساب"),
        "boss_whatsapp_saved" to Pair("Boss WhatsApp number saved successfully!", "تم حفظ رقم واتساب المدير بنجاح!"),
        "boss_number_saved_hint" to Pair("Saved Boss WhatsApp", "رقم واتساب المدير المحفوظ"),
        "boss_number_not_set" to Pair("Boss WhatsApp number is not configured in Settings", "رقم واتساب المدير غير محدد في الإعدادات"),
        "whatsapp_opened" to Pair("Opening WhatsApp with prefilled message...", "جاري فتح واتساب مع الرسالة الجاهزة..."),
        "test_whatsapp" to Pair("Test Boss WhatsApp", "تجربة مراسلة المدير"),
        "not_configured" to Pair("Not configured yet", "لم يتم التحديد بعد")
    )

    fun get(key: String, isArabic: Boolean): String {
        val pair = translations[key] ?: return key
        return if (isArabic) pair.second else pair.first
    }
}
