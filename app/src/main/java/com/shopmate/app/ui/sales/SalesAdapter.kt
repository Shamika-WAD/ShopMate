package com.shopmate.app.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.databinding.ItemSaleBinding
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.DateUtils

class SalesAdapter(
    private val onClick: (Sale) -> Unit,
) : ListAdapter<Sale, SalesAdapter.SaleViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Sale>() {
            override fun areItemsTheSame(old: Sale, new: Sale) = old.id == new.id
            override fun areContentsTheSame(old: Sale, new: Sale) = old == new
        }
    }

    class SaleViewHolder(val binding: ItemSaleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SaleViewHolder(
        ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = getItem(position)
        val ctx = holder.itemView.context
        val b = holder.binding

        b.tvSaleDate.text = DateUtils.formatDateTime(sale.timestamp)
        b.tvSaleItems.text =
            ctx.getString(R.string.sale_items_summary, sale.itemCount, sale.paymentMethod)
        b.tvSaleTotal.text = CurrencyFormatter.format(ctx, sale.total)

        b.root.setOnClickListener { onClick(sale) }
    }
}
