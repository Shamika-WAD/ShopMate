package com.shopmate.app.util

import com.shopmate.app.data.local.entity.Product

/**
 * Word-prefix matcher shared by the Billing and Inventory search bars.
 *
 * Typing "m" shows only products where *a word starts with* "m"
 * (never items that just contain "m" somewhere in the middle).
 * Ordering: products whose FIRST word matches come first, matches on
 * later words come next, then barcode / category matches last.
 */
object SearchMatcher {

    /** 0 = first word matches · 1 = another word matches · 2 = barcode/category · null = no match */
    private fun rank(product: Product, query: String): Int? {
        val words = product.name.split(' ', ',', '-').filter { it.isNotBlank() }
        if (words.firstOrNull()?.startsWith(query, ignoreCase = true) == true) return 0
        if (words.drop(1).any { it.startsWith(query, ignoreCase = true) }) return 1
        if (product.barcode.startsWith(query) ||
            product.category.startsWith(query, ignoreCase = true)
        ) {
            return 2
        }
        return null
    }

    /** Matches [query] against [products], best matches first. Blank query returns everything. */
    fun filter(products: List<Product>, query: String): List<Product> {
        val q = query.trim()
        if (q.isEmpty()) return products
        return products
            .mapNotNull { p -> rank(p, q)?.let { p to it } }
            .sortedWith(compareBy({ it.second }, { it.first.name.lowercase() }))
            .map { it.first }
    }
}
