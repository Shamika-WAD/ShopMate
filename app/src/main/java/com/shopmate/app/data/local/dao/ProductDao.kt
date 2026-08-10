package com.shopmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shopmate.app.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * All database operations for products.
 * Functions returning Flow re-emit automatically when the table changes,
 * so the UI always shows fresh data.
 */
@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Product?

    /** Every product whose stock is at or below the alert level. */
    @Query("SELECT * FROM products WHERE stockQty <= lowStockThreshold ORDER BY stockQty ASC")
    fun observeLowStock(): Flow<List<Product>>

    /** Products with an expiry date on or before [untilMillis] (already expired included). */
    @Query("SELECT * FROM products WHERE expiryDate IS NOT NULL AND expiryDate <= :untilMillis ORDER BY expiryDate ASC")
    fun observeExpiring(untilMillis: Long): Flow<List<Product>>

    /** True when another product already uses this barcode. */
    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE barcode = :barcode AND id != :excludeId)")
    suspend fun barcodeExists(barcode: String, excludeId: Long): Boolean
}
