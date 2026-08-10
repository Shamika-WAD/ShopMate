package com.shopmate.app.ui.billing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shopmate.app.databinding.ItemCartBinding
import com.shopmate.app.util.CurrencyFormatter

class CartAdapter(
    private val onPlus: (CartLine) -> Unit,
    private val onMinus: (CartLine) -> Unit
) : ListAdapter<CartLine, CartAdapter.CartViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CartLine>() {
            override fun areItemsTheSame(old: CartLine, new: CartLine) =
                old.product.id == new.product.id

            override fun areContentsTheSame(old: CartLine, new: CartLine) = old == new
        }
    }

    inner class CartViewHolder(val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CartViewHolder(
        ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val line = getItem(position)
        val ctx = holder.itemView.context
        val b = holder.binding

        b.tvName.text = line.product.name
        b.tvUnitPrice.text = "${CurrencyFormatter.format(ctx, line.product.sellPrice)} each"
        b.tvQty.text = line.qty.toString()
        b.tvLineTotal.text = CurrencyFormatter.format(ctx, line.lineTotal)

        b.btnPlus.setOnClickListener { onPlus(line) }
        b.btnMinus.setOnClickListener { onMinus(line) }
    }
}
