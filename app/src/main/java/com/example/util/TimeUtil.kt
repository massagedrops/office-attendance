package com.example.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object TimeUtil {
    val RIYADH_ZONE: ZoneId = ZoneId.of("Asia/Riyadh")

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    val TIME_FORMATTER_12H: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)
    val TIME_FORMATTER_24H: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)
    val SHORT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    fun now(): ZonedDateTime = ZonedDateTime.now(RIYADH_ZONE)

    fun todayDateString(): String = now().format(DATE_FORMATTER)

    fun currentTimeString(): String = now().format(TIME_FORMATTER_12H)

    fun formatMillisToDate(millis: Long): String {
        return Instant.ofEpochMilli(millis).atZone(RIYADH_ZONE).format(DATE_FORMATTER)
    }

    fun formatMillisToTime(millis: Long): String {
        return Instant.ofEpochMilli(millis).atZone(RIYADH_ZONE).format(TIME_FORMATTER_12H)
    }

    fun formatMillisToShortTime(millis: Long): String {
        return Instant.ofEpochMilli(millis).atZone(RIYADH_ZONE).format(SHORT_TIME_FORMATTER)
    }

    /**
     * Calculates actual working hours as a rounded decimal (e.g. 8.5)
     */
    fun calculateWorkingHours(checkInMillis: Long, checkOutMillis: Long): Double {
        if (checkOutMillis <= checkInMillis) return 0.0
        val duration = Duration.ofMillis(checkOutMillis - checkInMillis)
        val minutes = duration.toMinutes()
        val hours = minutes / 60.0
        return (hours * 100).roundToInt() / 100.0
    }

    /**
     * Determines late arrival in minutes.
     * shiftStartTime is "HH:mm" (e.g. "14:00")
     * If check-in is after shift start + grace minutes, returns the late minutes difference.
     */
    fun calculateLateMinutes(
        checkInEpochMillis: Long,
        shiftStartTime: String,
        graceMinutes: Int,
        attendanceDateStr: String
    ): Int {
        try {
            val parts = shiftStartTime.split(":")
            if (parts.size < 2) return 0
            val startHour = parts[0].toInt()
            val startMin = parts[1].toInt()

            val date = LocalDate.parse(attendanceDateStr, DATE_FORMATTER)
            val scheduledStart = ZonedDateTime.of(date, LocalTime.of(startHour, startMin), RIYADH_ZONE)
            val graceThreshold = scheduledStart.plusMinutes(graceMinutes.toLong())

            val actualCheckIn = Instant.ofEpochMilli(checkInEpochMillis).atZone(RIYADH_ZONE)

            if (actualCheckIn.isAfter(graceThreshold)) {
                val lateDuration = Duration.between(scheduledStart, actualCheckIn)
                return max(0, lateDuration.toMinutes().toInt())
            }
        } catch (_: Exception) {
            // Fallback safe
        }
        return 0
    }

    /**
     * Formats hours decimal to readable "8h 30m"
     */
    fun formatHoursToDurationString(hours: Double, isArabic: Boolean): String {
        val totalMinutes = (hours * 60).roundToInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (isArabic) {
            if (h > 0) "$h س $m د" else "$m د"
        } else {
            if (h > 0) "${h}h ${m}m" else "${m}m"
        }
    }

    /**
     * Formats duration between check-in and current time into live timer string:
     * e.g., "12h 35m 20s — In Progress" or Arabic equivalent
     */
    fun formatLiveWorkingDuration(checkInMillis: Long, nowMillis: Long = System.currentTimeMillis(), isArabic: Boolean): String {
        if (checkInMillis <= 0L) {
            return if (isArabic) "0 ث — قيد العمل" else "0s — In Progress"
        }
        val elapsedMillis = max(0L, nowMillis - checkInMillis)
        val totalSeconds = elapsedMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val inProgressLabel = if (isArabic) "قيد العمل" else "In Progress"
        val timeFormatted = if (isArabic) {
            if (hours > 0) "${hours}س ${minutes}د ${seconds}ث"
            else if (minutes > 0) "${minutes}د ${seconds}ث"
            else "${seconds}ث"
        } else {
            if (hours > 0) "${hours}h ${minutes}m ${seconds}s"
            else if (minutes > 0) "${minutes}m ${seconds}s"
            else "${seconds}s"
        }
        return "$timeFormatted — $inProgressLabel"
    }

    /**
     * Formats completed duration:
     * e.g., "11h 45m — Completed" or Arabic equivalent
     */
    fun formatCompletedWorkingDuration(hours: Double, isArabic: Boolean): String {
        val durationStr = formatHoursToDurationString(hours, isArabic)
        val completedLabel = if (isArabic) "مكتمل" else "Completed"
        return "$durationStr — $completedLabel"
    }

    /**
     * Parses a user-entered or formatted time string (e.g. "08:30:00", "08:30", "08:30:00 AM")
     * in conjunction with an attendance date string (yyyy-MM-dd) into epoch millis in Asia/Riyadh.
     * Also supports next-day checkout handling if checkout time is earlier than check-in time.
     */
    fun parseDateTimeToMillis(
        dateStr: String,
        timeStr: String,
        referenceCheckInMillis: Long? = null
    ): Long? {
        val trimmed = timeStr.trim()
        if (trimmed.isBlank()) return null

        val parsers = listOf(
            TIME_FORMATTER_12H,
            TIME_FORMATTER_24H,
            SHORT_TIME_FORMATTER,
            DateTimeFormatter.ofPattern("H:m:s", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("H:m", Locale.ENGLISH)
        )

        var localTime: LocalTime? = null
        for (parser in parsers) {
            try {
                localTime = LocalTime.parse(trimmed, parser)
                break
            } catch (_: Exception) {
            }
        }
        if (localTime == null) return null

        return try {
            val date = LocalDate.parse(dateStr, DATE_FORMATTER)
            var zdt = ZonedDateTime.of(date, localTime, RIYADH_ZONE)

            // If a reference check-in millis is provided (e.g., overnight shift where checkout crosses midnight)
            if (referenceCheckInMillis != null && zdt.toInstant().toEpochMilli() < referenceCheckInMillis) {
                // Checkout occurred the following calendar day
                zdt = zdt.plusDays(1)
            }
            zdt.toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
