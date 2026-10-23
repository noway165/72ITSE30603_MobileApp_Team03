package vn.edu.vlu.moneymanager.util

import java.util.Calendar
import java.util.TimeZone

object DateUtils {

    /**
     * MaterialDatePicker returns the picked day as UTC midnight.
     * Converts it to the same calendar day in the device time zone, at [hour]:[minute].
     */
    fun fromPickerUtc(utcMillis: Long, hour: Int = 12, minute: Int = 0): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute)
        }.timeInMillis
    }

    /** Inverse of [fromPickerUtc]: the local day of [localMillis] as UTC midnight, to preselect it in the picker. */
    fun toPickerUtc(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun endOfDay(localMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = localMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
