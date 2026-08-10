package com.shopmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Header of one completed bill. The line items live in [SaleItem].
 */
@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: String = "CASH", // CASH or CARD
    val itemCount: Int
)
