package vn.edu.vlu.moneymanager

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import vn.edu.vlu.moneymanager.data.AppContainer

/** Gives AppContainer the application Context that Room needs to open the database file. */
class FinanceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        // The saved theme choice applies from the first screen, not only once Profile is opened
        AppContainer.settingsRepository.darkMode.value?.let { dark ->
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
