package com.shopmate.app.data.repository

import com.shopmate.app.data.local.dao.ProductDao
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

/**
 * A clean middle layer between ViewModels and the database.
 * ViewModels never touch DAOs directly.
 */
class ProductRepository(private val productDao: ProductDao) {

    fun observeAll(): Flow<List<Product>> = productDao.observeAll()

    suspend fun getAllOnce(): List<Product> = productDao.getAllOnce()

    fun observeLowStock(): Flow<List<Product>> = productDao.observeLowStock()

    fun observeExpiring(withinDays: Int): Flow<List<Product>> {
        val until = System.currentTimeMillis() + (withinDays * DateUtils.DAY_MILLIS)
        return productDao.observeExpiring(until)
    }

    suspend fun getById(id: Long): Product? = productDao.getById(id)

    suspend fun findByBarcode(barcode: String): Product? = productDao.findByBarcode(barcode)

    suspend fun barcodeExists(barcode: String, excludeId: Long): Boolean =
        productDao.barcodeExists(barcode, excludeId)

    suspend fun insert(product: Product): Long = productDao.insert(product)

    suspend fun insertAll(products: List<Product>) = productDao.insertAll(products)

    suspend fun update(product: Product) =
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(product: Product) = productDao.delete(product)
}
