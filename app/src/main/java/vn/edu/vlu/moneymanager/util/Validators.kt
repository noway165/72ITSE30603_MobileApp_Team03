package vn.edu.vlu.moneymanager.util

/** Pure-Kotlin input checks shared by ViewModels (no android.util.Patterns, so they run in unit tests). */
object Validators {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    /** At least 8 characters with both letters and digits. */
    fun isValidPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

    /** Parses "85000", "85,000" or "12.5"; returns null for empty, invalid or non-positive input. */
    fun parseAmount(text: String): Double? =
        text.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
}
