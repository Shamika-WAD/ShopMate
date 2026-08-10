package com.shopmate.app.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.ItemProductCompactBinding
import com.shopmate.app.util.DateUtils

/** Which warning style this list instance renders. */
enum class AlertMode { LOW_STOCK, EXPIRING }

class AlertAdapter(
    private val mode: AlertMode,
    private val onClick: (Product) -> Unit,
) : ListAdapter<Product, AlertAdapter.AlertViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
            override fun areContentsTheSame(old: Product, new: Product) = old == new
        }
    }

    class AlertViewHolder(val binding: ItemProductCompactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AlertViewHolder(
        ItemProductCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val product = getItem(position)
        val ctx = holder.itemView.context
        val b = holder.binding

        b.tvName.text = product.name

        when (mode) {
            AlertMode.LOW_STOCK -> {
                b.tvReason.text =
                    ctx.getString(R.string.only_left, product.stockQty, product.lowStockThreshold)
                b.tvReason.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
            }

            AlertMode.EXPIRING -> {
                val expiry = product.expiryDate ?: return
                val days = DateUtils.daysUntil(expiry)
                val date = DateUtils.formatDate(expiry)
                b.tvReason.text = when {
                    days < 0 -> ctx.getString(R.string.expired_days_ago, -days, date)
                    days == 0 -> ctx.getString(R.string.expires_today, date)
                    else -> ctx.getString(R.string.expires_in, days, date)
                }
                val color = if (days < 0) R.color.danger else R.color.warning
                b.tvReason.setTextColor(ContextCompat.getColor(ctx, color))
            }
        }

        b.root.setOnClickListener { onClick(product) }
    }
}
