package vn.edu.vlu.moneymanager

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import vn.edu.vlu.moneymanager.databinding.ActivityMainBinding

/** Shell of the app: toolbar + NavHostFragment + bottom navigation. All screens are Fragments with their own ViewModel. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val appBarConfiguration = AppBarConfiguration(
        setOf(
            R.id.dashboardFragment,
            R.id.transactionListFragment,
            R.id.budgetFragment,
            R.id.reportFragment,
            R.id.profileFragment
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Portrait layout has bottomNav, layout-land has navRail: ViewBinding makes both nullable.
        binding.bottomNav?.setupWithNavController(navController)
        binding.navRail?.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ -> highlightParentTab(destination.id) }
    }

    /**
     * Categories and the currency converter are not tabs. NavigationUI then leaves the tab highlight alone,
     * and after a rotation the recreated bar falls back to its first item (Home). Highlight the tab the
     * screen was opened from instead: the top-level destination still in the back stack.
     */
    private fun highlightParentTab(@IdRes destinationId: Int) {
        val tabs = appBarConfiguration.topLevelDestinations
        if (destinationId in tabs) return
        // Dashboard is the start destination, so it is under every other tab: check the others first.
        val parentTab = tabs.firstOrNull { it != R.id.dashboardFragment && isInBackStack(it) } ?: R.id.dashboardFragment
        binding.bottomNav?.menu?.findItem(parentTab)?.isChecked = true
        binding.navRail?.menu?.findItem(parentTab)?.isChecked = true
    }

    private fun isInBackStack(@IdRes destinationId: Int): Boolean =
        runCatching { navController.getBackStackEntry(destinationId) }.isSuccess

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    /** Used by Dashboard shortcuts ("See all", "Budgets") to switch tab like a bottom-nav tap. */
    fun openTab(@IdRes destinationId: Int) {
        binding.bottomNav?.selectedItemId = destinationId
        binding.navRail?.selectedItemId = destinationId
    }
}
