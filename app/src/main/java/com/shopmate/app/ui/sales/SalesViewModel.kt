package com.shopmate.app.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.repository.SaleRepository

class SalesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SaleRepository(AppDatabase.getInstance(app).saleDao())

    val sales: LiveData<List<Sale>> = repo.observeAll().asLiveData()
}
