package com.shopmate.app.ui.receipt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem
import com.shopmate.app.data.repository.SaleRepository
import com.shopmate.app.util.ReceiptBuilder
import kotlinx.coroutines.launch

class ReceiptViewModel(app: Application) : AndroidViewModel(app) {

    data class ReceiptState(
        val sale: Sale,
        val items: List<SaleItem>,
        val receiptText: String
    )

    private val repo = SaleRepository(AppDatabase.getInstance(app).saleDao())

    private val _state = MutableLiveData<ReceiptState?>()
    val state: LiveData<ReceiptState?> = _state

    fun load(saleId: Long) {
        if (_state.value != null) return
        viewModelScope.launch {
            val sale = repo.getById(saleId)
            if (sale != null) {
                val items = repo.getItems(sale.id)
                _state.value = ReceiptState(
                    sale = sale,
                    items = items,
                    receiptText = ReceiptBuilder.build(getApplication(), sale, items)
                )
            }
        }
    }
}
