package com.shopmate.app.ui.barcode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.repository.ProductRepository
import kotlinx.coroutines.launch

class BarcodeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.getInstance(app).productDao())

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    fun load(productId: Long) {
        viewModelScope.launch { _product.value = repo.getById(productId) }
    }
}
