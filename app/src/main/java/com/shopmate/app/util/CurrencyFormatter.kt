package com.shopmate.app.util

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/**
 * Formats money with the currency symbol from Settings, e.g. "Rs. 1,250.00".
 */
object CurrencyFormatter {

    fun format(context: Context, amount: Double): String {
        val symbol = Settings(context).currencySymbol
        val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$symbol ${number.format(amount)}"
    }
}
