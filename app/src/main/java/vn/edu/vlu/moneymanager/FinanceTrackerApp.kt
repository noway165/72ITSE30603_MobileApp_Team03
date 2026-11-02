package vn.edu.vlu.moneymanager

import android.app.Application
import vn.edu.vlu.moneymanager.data.AppContainer

/** Gives AppContainer the application Context that Room needs to open the database file. */
class FinanceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
