package com.shopmate.app.ui.alerts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.repository.ProductRepository
import com.shopmate.app.util.Settings

class AlertsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.getInstance(app).productDao())
    private val settings = Settings(app)

    val lowStock: LiveData<List<Product>> = repo.observeLowStock().asLiveData()

    val expiring: LiveData<List<Product>> =
        repo.observeExpiring(settings.expiryWarnDays).asLiveData()
}
