package com.shopmate.app.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.ItemProductBinding
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.DateUtils
import com.shopmate.app.util.Settings

class InventoryAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onStockPlus: (Product) -> Unit,
    private val onStockMinus: (Product) -> Unit,
) : ListAdapter<Product, InventoryAdapter.ProductViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
            override fun areContentsTheSame(old: Product, new: Product) = old == new
        }
    }

    class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProductViewHolder(
        ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        val ctx = holder.itemView.context
        val b = holder.binding

        b.tvName.text = product.name
        b.tvCategory.text = product.category.ifBlank { ctx.getString(R.string.no_category) }
        b.tvBarcode.text = product.barcode
        b.tvPrice.text = CurrencyFormatter.format(ctx, product.sellPrice)

        // Stock (red when at/below the alert level)
        val isLow = product.stockQty <= product.lowStockThreshold
        b.tvStock.text = ctx.getString(R.string.stock_count, product.stockQty)
        b.tvLowBadge.isVisible = isLow
        val stockColor = if (isLow) {
            ContextCompat.getColor(ctx, R.color.danger)
        } else {
            MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        b.tvStock.setTextColor(stockColor)

        // Expiry date (orange when it is close)
        if (product.expiryDate != null) {
            b.tvExpiry.isVisible = true
            b.tvExpiry.text = ctx.getString(R.string.exp_label, DateUtils.formatDate(product.expiryDate))
            val warnDays = Settings(ctx).expiryWarnDays
            val close = DateUtils.daysUntil(product.expiryDate) <= warnDays
            val expiryColor = if (close) {
                ContextCompat.getColor(ctx, R.color.warning)
            } else {
                MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)
            }
            b.tvExpiry.setTextColor(expiryColor)
        } else {
            b.tvExpiry.isVisible = false
        }

        // One-tap restock buttons directly on the row
        b.btnStockMinus.setOnClickListener { onStockMinus(product) }
        b.btnStockPlus.setOnClickListener { onStockPlus(product) }

        b.root.setOnClickListener { onEdit(product) }
        b.btnEdit.setOnClickListener { onEdit(product) }
        b.btnDelete.setOnClickListener { onDelete(product) }
    }
}
