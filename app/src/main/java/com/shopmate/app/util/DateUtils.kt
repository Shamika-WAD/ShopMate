package com.shopmate.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date helpers shared across the app.
 * Dates are stored as epoch millis; display is always local time.
 */
object DateUtils {

    const val DAY_MILLIS: Long = 86_400_000L

    private val dateFormat get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormat get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    /**
     * Whole calendar days from today until [dateMillis].
     * 0 = expires today, negative = already expired.
     */
    fun daysUntil(dateMillis: Long): Int {
        val cal = Calendar.getInstance()
        zeroTimeOfDay(cal)
        val todayMidnight = cal.timeInMillis
        cal.timeInMillis = dateMillis
        zeroTimeOfDay(cal)
        return ((cal.timeInMillis - todayMidnight) / DAY_MILLIS).toInt()
    }

    /** [start, end) millis covering "today" in local time (for sales queries). */
    fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        zeroTimeOfDay(cal)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun zeroTimeOfDay(cal: Calendar) {
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
    }
}
