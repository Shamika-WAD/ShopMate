package com.shopmate.app.util

import android.content.Context
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.local.entity.Sale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes inventory / sales tables to CSV files inside the app cache,
 * ready to be shared via FileProvider (opens in Excel).
 */
object CsvExporter {

    suspend fun exportProducts(context: Context, products: List<Product>): File =
        withContext(Dispatchers.IO) {
            val file = newExportFile(context, "products")
            file.bufferedWriter().use { writer ->
                writer.appendLine("barcode,name,category,unit,cost_price,selling_price,stock_qty,low_stock_threshold,expiry_date")
                products.forEach { p ->
                    writer.appendLine(
                        listOf(
                            p.barcode.quoted(),
                            p.name.quoted(),
                            p.category.quoted(),
                            p.unit.quoted(),
                            p.costPrice?.toString() ?: "",
                            p.sellPrice.toString(),
                            p.stockQty.toString(),
                            p.lowStockThreshold.toString(),
                            p.expiryDate?.let { DateUtils.formatDate(it) } ?: ""
                        ).joinToString(",")
                    )
                }
            }
            file
        }

    suspend fun exportSales(context: Context, sales: List<Sale>): File =
        withContext(Dispatchers.IO) {
            val file = newExportFile(context, "sales")
            file.bufferedWriter().use { writer ->
                writer.appendLine("id,date,items,subtotal,discount,total,payment_method")
                sales.forEach { s ->
                    writer.appendLine(
                        listOf(
                            s.id.toString(),
                            DateUtils.formatDateTime(s.timestamp).quoted(),
                            s.itemCount.toString(),
                            s.subtotal.toString(),
                            s.discount.toString(),
                            s.total.toString(),
                            s.paymentMethod
                        ).joinToString(",")
                    )
                }
            }
            file
        }

    private fun newExportFile(context: Context, prefix: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "${prefix}_$stamp.csv")
    }

    /** Wraps text in quotes so commas inside names don't break the CSV. */
    private fun String.quoted(): String = "\"" + replace("\"", "\"\"") + "\""
}
