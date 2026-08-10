package com.shopmate.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.core.view.get
import androidx.core.view.size
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.navOptions
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.databinding.ActivityMainBinding
import com.shopmate.app.util.DateUtils
import com.shopmate.app.util.Settings
import com.shopmate.app.util.ThemeUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applySaved(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // The 5 bottom-nav destinations are "top level" (no Up arrow on them)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.inventoryFragment,
                R.id.billingFragment,
                R.id.alertsFragment,
                R.id.salesFragment,
            ),
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Bottom tab taps -> navigate via NavigationUI (handles back stack &
        // state restore). Done manually (instead of setupWithNavController) so
        // in-app action navigations can never leave the tabs in a broken state.
        // Bottom tab taps -> always show that tab's ROOT screen, no matter
        // what (Settings, product form, receipt...) is currently on top.
        binding.bottomNav.setOnItemSelectedListener { item ->
            val id = item.itemId
            when {
                // Already on this tab: keep it as is (e.g. a bill in progress)
                navController.currentDestination?.id == id -> true

                id == R.id.dashboardFragment -> {
                    // Home: just remove everything above it
                    navController.popBackStack(R.id.dashboardFragment, inclusive = false)
                    true
                }

                else -> {
                    // Remove any old copy of this tab + nested pages, then
                    // open a fresh one on top of Dashboard.
                    navController.popBackStack(id, inclusive = true)
                    navController.popBackStack(R.id.dashboardFragment, inclusive = false)
                    navController.navigate(id, null, navOptions { launchSingleTop = true })
                    true
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Full-screen feel while scanning: hide the bottom bar there
            binding.bottomNav.isVisible = destination.id != R.id.scannerFragment
            // Keep the highlighted tab in sync no matter how we navigated
            syncSelectedTab(destination.id)
        }

        watchAlertsBadge()
    }

    /** Any screen can switch bottom tabs as if the user tapped the icon. */
    fun openTab(destinationId: Int) {
        if (binding.bottomNav.selectedItemId != destinationId) {
            binding.bottomNav.selectedItemId = destinationId
        }
    }

    /**
     * Which bottom tab should be highlighted for a screen. Nested screens
     * belong to a tab: the product form & barcode page -> Inventory,
     * scanner & bill preview -> Billing.
     */
    private fun tabFor(destinationId: Int): Int = when (destinationId) {
        R.id.productFormFragment, R.id.barcodeFragment -> R.id.inventoryFragment
        R.id.scannerFragment, R.id.receiptFragment -> R.id.billingFragment
        else -> destinationId
    }

    private fun syncSelectedTab(destinationId: Int) {
        val tabId = tabFor(destinationId)
        val menu = binding.bottomNav.menu
        for (i in 0 until menu.size) {
            val item = menu[i]
            if (item.itemId == tabId) {
                if (binding.bottomNav.selectedItemId != tabId) {
                    item.isChecked = true
                }
                return
            }
        }
    }

    /** Shows a red count badge on the Alerts tab from anywhere in the app. */
    private fun watchAlertsBadge() {
        val productDao = AppDatabase.getInstance(this).productDao()
        val until = System.currentTimeMillis() +
                (Settings(this).expiryWarnDays * DateUtils.DAY_MILLIS)

        lifecycleScope.launch {
            combine(
                productDao.observeLowStock(),
                productDao.observeExpiring(until),
            ) { low, expiring -> low.size + expiring.size }
                .collect { count ->
                    val badge = binding.bottomNav.getOrCreateBadge(R.id.alertsFragment)
                    badge.isVisible = count > 0
                    if (count > 0) badge.number = count
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
