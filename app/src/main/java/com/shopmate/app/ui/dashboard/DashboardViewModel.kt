package com.shopmate.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.repository.ProductRepository
import com.shopmate.app.data.repository.SaleRepository
import com.shopmate.app.util.DateUtils
import com.shopmate.app.util.Settings
import kotlinx.coroutines.flow.map

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val productRepo = ProductRepository(db.productDao())
    private val saleRepo = SaleRepository(db.saleDao())
    private val settings = Settings(app)

    private val today = DateUtils.todayRange()

    val shopName: String get() = settings.shopName

    val todaySales: LiveData<Double> =
        saleRepo.observeTodayTotal(today.first, today.second).asLiveData()

    val todayBills: LiveData<Int> =
        saleRepo.observeTodayCount(today.first, today.second).asLiveData()

    val lowStockCount: LiveData<Int> =
        productRepo.observeLowStock().map { it.size }.asLiveData()

    val expiringCount: LiveData<Int> =
        productRepo.observeExpiring(settings.expiryWarnDays).map { it.size }.asLiveData()
}
