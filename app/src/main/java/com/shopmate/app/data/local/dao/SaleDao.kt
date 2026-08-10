package com.shopmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SaleDao {

    @Insert
    abstract suspend fun insertSale(sale: Sale): Long

    @Insert
    abstract suspend fun insertItems(items: List<SaleItem>)

    @Query("UPDATE products SET stockQty = stockQty - :qty WHERE id = :productId")
    abstract suspend fun decrementStock(productId: Long, qty: Int)

    /**
     * Saves the bill and reduces stock in ONE transaction -
     * either everything succeeds or nothing is written.
     * Returns the new sale id (used as the receipt number).
     */
    @Transaction
    open suspend fun checkout(sale: Sale, items: List<SaleItem>): Long {
        val saleId = insertSale(sale)
        insertItems(items.map { it.copy(saleId = saleId) })
        items.forEach { item ->
            item.productId?.let { productId -> decrementStock(productId, item.qty) }
        }
        return saleId
    }

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    abstract fun observeAll(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    abstract suspend fun getAllOnce(): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id")
    abstract suspend fun getById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id ASC")
    abstract suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    /** Total money taken between [dayStart] (inclusive) and [dayEnd] (exclusive). */
    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    abstract fun observeTodayTotal(dayStart: Long, dayEnd: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM sales WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    abstract fun observeTodayCount(dayStart: Long, dayEnd: Long): Flow<Int>
}
