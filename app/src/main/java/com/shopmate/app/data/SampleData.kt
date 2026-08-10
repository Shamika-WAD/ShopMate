package com.shopmate.app.data

import com.shopmate.app.data.local.entity.Product

/**
 * Demo products inserted from the Settings screen ("Load demo products").
 * Two items are intentionally low stock and two are near expiry,
 * so every screen of the app has something to show.
 *
 * To test scanning, generate barcodes for these numbers with any free
 * online "EAN-13 barcode generator" and point the camera at them.
 */
object SampleData {

    fun products(): List<Product> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            Product(
                barcode = "8906001230001",
                name = "Samba Rice 5kg",
                category = "Grocery",
                unit = "bag",
                costPrice = 1180.0,
                sellPrice = 1450.0,
                stockQty = 8,
                lowStockThreshold = 5,
                expiryDate = now + (180 * day),
            ),
            Product(
                barcode = "8906001230002",
                name = "Red Dhal 1kg",
                category = "Grocery",
                unit = "pack",
                costPrice = 390.0,
                sellPrice = 480.0,
                stockQty = 20,
                lowStockThreshold = 6,
                expiryDate = now + 120 * day
            ),
            Product(
                barcode = "8906001230003",
                name = "Milk Powder 400g",
                category = "Grocery",
                unit = "pack",
                costPrice = 1080.0,
                sellPrice = 1240.0,
                stockQty = 4,                 // LOW STOCK (alert at 5)
                lowStockThreshold = 5,
                expiryDate = now + 240 * day
            ),
            Product(
                barcode = "8906001230004",
                name = "Laundry Soap 110g",
                category = "Household",
                unit = "pcs",
                costPrice = 110.0,
                sellPrice = 145.0,
                stockQty = 30,
                lowStockThreshold = 10
                // no expiry on purpose
            ),
            Product(
                barcode = "8906001230005",
                name = "Yoghurt Cup 80ml",
                category = "Dairy",
                unit = "cup",
                costPrice = 70.0,
                sellPrice = 90.0,
                stockQty = 15,
                lowStockThreshold = 5,
                expiryDate = now + 4 * day     // EXPIRING SOON
            ),
            Product(
                barcode = "8906001230006",
                name = "Sandwich Bread",
                category = "Bakery",
                unit = "loaf",
                costPrice = 130.0,
                sellPrice = 160.0,
                stockQty = 3,                  // LOW STOCK
                lowStockThreshold = 4,
                expiryDate = now + day         // EXPIRING TOMORROW
            ),
            Product(
                barcode = "8906001230007",
                name = "Cola 500ml",
                category = "Beverages",
                unit = "bottle",
                costPrice = 180.0,
                sellPrice = 220.0,
                stockQty = 24,
                lowStockThreshold = 10,
                expiryDate = now + 90 * day
            ),
            Product(
                barcode = "8906001230008",
                name = "Cream Biscuits 200g",
                category = "Snacks",
                unit = "pack",
                costPrice = 145.0,
                sellPrice = 180.0,
                stockQty = 18,
                lowStockThreshold = 6,
                expiryDate = now + 75 * day
            )
        )
    }
}
