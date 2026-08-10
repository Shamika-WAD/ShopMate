package com.shopmate.app.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopmate.app.R
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductFormViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.getInstance(app).productDao())

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    fun load(id: Long) {
        viewModelScope.launch { _product.value = repo.getById(id) }
    }

    /**
     * Creates a unique 12-digit in-store code starting with "20"
     * (the industry-reserved shop range, EAN-13 compatible) for
     * products that don't have their own barcode.
     */
    fun generateUniqueCode(onResult: (String) -> Unit) {
        viewModelScope.launch {
            var code: String
            do {
                code = "20" + "%010d".format(kotlin.random.Random.nextLong(0L, 9_999_999_999L))
            } while (repo.barcodeExists(code, -1L))
            onResult(code)
        }
    }

    /**
     * Saves a new or existing product (decided by product.id).
     * Refuses to save when the barcode belongs to a different product.
     */
    fun save(product: Product, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (repo.barcodeExists(product.barcode, product.id)) {
                onResult(false, getApplication<Application>().getString(R.string.barcode_used))
            } else {
                if (product.id > 0) repo.update(product) else repo.insert(product)
                onResult(true, getApplication<Application>().getString(R.string.product_saved))
            }
        }
    }
}
