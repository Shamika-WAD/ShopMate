package com.shopmate.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shopmate.app.data.SampleData
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.repository.ProductRepository
import com.shopmate.app.data.repository.SaleRepository
import com.shopmate.app.util.CsvExporter
import java.io.File

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val productRepo = ProductRepository(db.productDao())
    private val saleRepo = SaleRepository(db.saleDao())

    suspend fun loadDemo() = productRepo.insertAll(SampleData.products())

    suspend fun exportProducts(): File =
        CsvExporter.exportProducts(getApplication(), productRepo.getAllOnce())

    suspend fun exportSales(): File =
        CsvExporter.exportSales(getApplication(), saleRepo.getAllOnce())
}
