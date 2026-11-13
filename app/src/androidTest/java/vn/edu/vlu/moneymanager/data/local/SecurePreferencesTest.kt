package vn.edu.vlu.moneymanager.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository

/** Runs on a device: EncryptedSharedPreferences needs the Android Keystore. */
@RunWith(AndroidJUnit4::class)
class SecurePreferencesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun clear() {
        securePreferences(context).edit(commit = true) { clear() }
    }

    @Test
    fun settings_surviveANewRepository() {
        SettingsRepository(securePreferences(context)).apply {
            setDefaultCurrency("JPY")
            setDarkMode(true)
            setLastSync(42L)
        }

        val reopened = SettingsRepository(securePreferences(context))

        assertEquals("JPY", reopened.defaultCurrency.value)
        assertEquals(true, reopened.darkMode.value)
        assertEquals(42L, reopened.lastSyncMillis.value)
    }

    @Test
    fun file_holdsNoPlainKeysOrValues() {
        SettingsRepository(securePreferences(context)).setDefaultCurrency("JPY")

        val raw = context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE).all

        assertFalse(raw.keys.any { "default_currency" in it })
        assertFalse(raw.values.any { it.toString().contains("JPY") })
    }
}
