package com.shopmate.app.data.repository

import com.shopmate.app.data.local.dao.SaleDao
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem
import kotlinx.coroutines.flow.Flow

class SaleRepository(private val saleDao: SaleDao) {

    fun observeAll(): Flow<List<Sale>> = saleDao.observeAll()

    suspend fun getAllOnce(): List<Sale> = saleDao.getAllOnce()

    suspend fun getById(id: Long): Sale? = saleDao.getById(id)

    suspend fun getItems(saleId: Long): List<SaleItem> = saleDao.getItemsForSale(saleId)

    fun observeTodayTotal(dayStart: Long, dayEnd: Long): Flow<Double> =
        saleDao.observeTodayTotal(dayStart, dayEnd)

    fun observeTodayCount(dayStart: Long, dayEnd: Long): Flow<Int> =
        saleDao.observeTodayCount(dayStart, dayEnd)

    /** Saves the bill and decrements stock atomically. Returns the new sale id. */
    suspend fun checkout(sale: Sale, items: List<SaleItem>): Long =
        saleDao.checkout(sale, items)
}
