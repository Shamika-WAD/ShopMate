package com.shopmate.app.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.repository.ProductRepository
import com.shopmate.app.util.SearchMatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.getInstance(app).productDao())

    private val query = MutableStateFlow("")

    /**
     * All products, filtered in memory by the search box text.
     * Uses the same word-prefix matching as the Billing search bar.
     * Small shops have few hundred products, so this stays fast.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val products: LiveData<List<Product>> =
        query.flatMapLatest { q ->
            repo.observeAll().map { list -> SearchMatcher.filter(list, q) }
        }.asLiveData()

    fun setQuery(text: String) {
        query.value = text
    }

    fun delete(product: Product) {
        viewModelScope.launch { repo.delete(product) }
    }

    /** Quick +1/-1 buttons on the inventory row (never goes below zero). */
    fun adjustStock(product: Product, delta: Int) {
        viewModelScope.launch {
            val newQty = (product.stockQty + delta).coerceAtLeast(0)
            if (newQty != product.stockQty) {
                repo.update(product.copy(stockQty = newQty))
            }
        }
    }
}
