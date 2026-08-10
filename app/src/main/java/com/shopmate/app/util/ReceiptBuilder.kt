package com.shopmate.app.util

import android.content.Context
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem
import java.util.Locale

/**
 * Builds a plain-text receipt (monospace, 32 chars wide) that fits
 * nicely in a dialog and can be shared to WhatsApp / email etc.
 */
object ReceiptBuilder {

    private const val WIDTH = 32

    fun build(context: Context, sale: Sale, items: List<SaleItem>): String {
        val settings = Settings(context)
        val money = { value: Double -> CurrencyFormatter.format(context, value) }

        val sb = StringBuilder()

        fun center(text: String) {
            val trimmed = text.take(WIDTH)
            val pad = ((WIDTH - trimmed.length) / 2).coerceAtLeast(0)
            sb.append(" ".repeat(pad)).append(trimmed).append('\n')
        }

        fun row(left: String, right: String) {
            val space = (WIDTH - left.length - right.length).coerceAtLeast(1)
            sb.append(left).append(" ".repeat(space)).append(right).append('\n')
        }

        center(settings.shopName)
        if (settings.address.isNotBlank()) center(settings.address)
        if (settings.phone.isNotBlank()) center(settings.phone)
        center(DateUtils.formatDateTime(sale.timestamp))
        if (sale.id > 0) center("Receipt #${sale.id}")
        sb.append("-".repeat(WIDTH)).append('\n')

        items.forEach { item ->
            sb.append(item.productName.take(WIDTH)).append('\n')
            row(
                "  ${item.qty} x ${trimNumber(item.unitPrice)}",
                trimNumber(item.lineTotal),
            )
        }

        sb.append("-".repeat(WIDTH)).append('\n')
        row("Subtotal", money(sale.subtotal))
        if (sale.discount > 0) row("Discount", "- " + money(sale.discount))
        row("TOTAL", money(sale.total))
        row("Paid by", sale.paymentMethod)
        sb.append("-".repeat(WIDTH)).append('\n')
        if (settings.receiptFooter.isNotBlank()) center(settings.receiptFooter)
        return sb.toString()
    }

    /** 1200 -> "1200", 120.5 -> "120.50" (keeps receipt lines short). */
    private fun trimNumber(value: Double): String =
        if ((value % 1.0) == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value)
}
