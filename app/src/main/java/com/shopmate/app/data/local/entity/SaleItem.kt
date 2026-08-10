package com.shopmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One line of a bill. We copy the product name / barcode / price here so that
 * deleting or editing the product later does not change old receipts.
 */
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long?,     // kept for reports; stays if the product is deleted
    val productName: String,  // snapshot at time of sale
    val barcode: String,
    val unitPrice: Double,
    val qty: Int,
    val lineTotal: Double
)
