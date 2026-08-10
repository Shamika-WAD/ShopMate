package com.shopmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per product in the shop.
 * Barcode is unique so a scan always maps to exactly one product.
 * expiryDate == null means the product never expires (soap, bottles...).
 */
@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String = "",
    val unit: String = "pcs",           // pcs, kg, bottle, pack...
    val costPrice: Double? = null,      // buying price (optional, for future profit reports)
    val sellPrice: Double,
    val stockQty: Int = 0,
    val lowStockThreshold: Int = 5,     // warn when stockQty drops to this level
    val expiryDate: Long? = null,       // epoch millis at local midnight, or null
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
