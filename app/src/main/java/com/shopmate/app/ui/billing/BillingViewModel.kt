package com.shopmate.app.ui.billing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.shopmate.app.data.local.AppDatabase
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem
import com.shopmate.app.data.repository.ProductRepository
import com.shopmate.app.data.repository.SaleRepository
import kotlinx.coroutines.launch

/** One line of the cart = a product plus how many were scanned. */
data class CartLine(
    val product: Product,
    val qty: Int,
) {
    val lineTotal: Double get() = product.sellPrice * qty
}

/** One-shot events the fragment should react to exactly once. */
sealed interface BillingEvent {
    data class UnknownBarcode(val barcode: String) : BillingEvent
    data class OutOfStock(val productName: String) : BillingEvent

    /** Carries the new sale id so the bill preview screen can open it. */
    data class SaleCompleted(val saleId: Long) : BillingEvent
}

class BillingViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val productRepo = ProductRepository(db.productDao())
    private val saleRepo = SaleRepository(db.saleDao())

    /** Used by the "search product" picker. */
    val allProducts: LiveData<List<Product>> = productRepo.observeAll().asLiveData()

    private val _cart = MutableLiveData<List<CartLine>>(emptyList())
    val cart: LiveData<List<CartLine>> = _cart

    val subtotal: LiveData<Double> = _cart.map { lines -> lines.sumOf { it.lineTotal } }
    val itemCount: LiveData<Int> = _cart.map { lines -> lines.sumOf { it.qty } }

    val event = MutableLiveData<BillingEvent?>()

    /** Called by the fragment after an event has been shown. */
    fun clearEvent() {
        event.value = null
    }

    /** Entry point for a value coming from the barcode scanner. */
    fun onBarcodeScanned(code: String) {
        viewModelScope.launch {
            val product = productRepo.findByBarcode(code)
            when {
                product == null -> event.value = BillingEvent.UnknownBarcode(code)
                !tryAddToCart(product) -> event.value = BillingEvent.OutOfStock(product.name)
            }
        }
    }

    /**
     * Adds one unit of [product] to the cart.
     * Returns false when the cart already holds all available stock.
     */
    fun tryAddToCart(product: Product): Boolean {
        val lines = _cart.value.orEmpty().toMutableList()
        val index = lines.indexOfFirst { it.product.id == product.id }
        val currentQty = if (index >= 0) lines[index].qty else 0
        if ((currentQty + 1) > product.stockQty) return false

        if (index >= 0) lines[index] = lines[index].copy(qty = currentQty + 1)
        else lines.add(CartLine(product, 1))

        _cart.value = lines
        return true
    }

    fun increase(line: CartLine) {
        tryAddToCart(line.product) // silently stops at stock limit
    }

    fun decrease(line: CartLine) {
        val lines = _cart.value.orEmpty().toMutableList()
        val index = lines.indexOfFirst { it.product.id == line.product.id }
        if (index >= 0) {
            val newQty = lines[index].qty - 1
            if (newQty <= 0) lines.removeAt(index)
            else lines[index] = lines[index].copy(qty = newQty)
            _cart.value = lines
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    /**
     * Finalizes the sale: writes the bill, decrements stock,
     * clears the cart and emits the receipt text to display.
     */
    fun checkout(discount: Double, paymentMethod: String) {
        val lines = _cart.value.orEmpty()
        if (lines.isEmpty()) return

        viewModelScope.launch {
            val sub = lines.sumOf { it.lineTotal }
            val total = (sub - discount).coerceAtLeast(0.0)

            val sale = Sale(
                timestamp = System.currentTimeMillis(),
                subtotal = sub,
                discount = discount.coerceAtLeast(0.0),
                total = total,
                paymentMethod = paymentMethod,
                itemCount = lines.sumOf { it.qty }
            )
            val items = lines.map { line ->
                SaleItem(
                    saleId = 0, // replaced with the real id inside the transaction
                    productId = line.product.id,
                    productName = line.product.name,
                    barcode = line.product.barcode,
                    unitPrice = line.product.sellPrice,
                    qty = line.qty,
                    lineTotal = line.lineTotal
                )
            }

            val saleId = saleRepo.checkout(sale, items)

            _cart.value = emptyList()
            event.value = BillingEvent.SaleCompleted(saleId)
        }
    }
}
