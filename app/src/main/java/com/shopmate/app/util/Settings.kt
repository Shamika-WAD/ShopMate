package com.shopmate.app.util

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences for user preferences.
 * Edited from the Settings screen, read everywhere else.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var shopName: String
        get() = prefs.getString(KEY_SHOP_NAME, "My Shop") ?: "My Shop"
        set(value) = prefs.edit().putString(KEY_SHOP_NAME, value).apply()

    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY, "Rs.") ?: "Rs."
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    /** Products expiring within this many days appear in Alerts. */
    var expiryWarnDays: Int
        get() = prefs.getInt(KEY_EXPIRY_DAYS, 7)
        set(value) = prefs.edit().putInt(KEY_EXPIRY_DAYS, value).apply()

    /** Pre-filled "low stock alert at" value for new products. */
    var defaultLowStock: Int
        get() = prefs.getInt(KEY_LOW_STOCK, 5)
        set(value) = prefs.edit().putInt(KEY_LOW_STOCK, value).apply()

    /** MAC address of the selected Bluetooth thermal printer (null = none). */
    var printerAddress: String?
        get() = prefs.getString(KEY_PRINTER_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_PRINTER_ADDRESS, value).apply()

    var printerName: String?
        get() = prefs.getString(KEY_PRINTER_NAME, null)
        set(value) = prefs.edit().putString(KEY_PRINTER_NAME, value).apply()

    /** 0 = follow phone setting · 1 = always light · 2 = always dark */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var address: String
        get() = prefs.getString(KEY_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ADDRESS, value).apply()

    var phone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    /** Thank-you message at the bottom of every bill. */
    var receiptFooter: String
        get() = prefs.getString(KEY_RECEIPT_FOOTER, DEFAULT_FOOTER) ?: DEFAULT_FOOTER
        set(value) = prefs.edit().putString(KEY_RECEIPT_FOOTER, value).apply()

    private companion object {
        const val KEY_SHOP_NAME = "shop_name"
        const val KEY_CURRENCY = "currency"
        const val KEY_EXPIRY_DAYS = "expiry_days"
        const val KEY_LOW_STOCK = "low_stock_default"
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_PRINTER_NAME = "printer_name"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ADDRESS = "shop_address"
        const val KEY_PHONE = "shop_phone"
        const val KEY_RECEIPT_FOOTER = "receipt_footer"
        const val DEFAULT_FOOTER = "Thank you! Please come again"
    }
}
