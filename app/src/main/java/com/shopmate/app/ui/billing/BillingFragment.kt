package com.shopmate.app.ui.billing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.FragmentBillingBinding
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.ScannerContract
import com.shopmate.app.util.SearchMatcher

class BillingFragment : Fragment() {

    private var _binding: FragmentBillingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BillingViewModel by activityViewModels()

    /** Cache of all products for the search picker (always up to date). */
    private var allProducts: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CartAdapter(
            onPlus = { viewModel.increase(it) }
        ) { viewModel.decrease(it) }
        binding.recyclerCart.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCart.adapter = adapter

        // Observed here so the product search ALWAYS has data
        viewModel.allProducts.observe(viewLifecycleOwner) { allProducts = it }

        viewModel.cart.observe(viewLifecycleOwner) { lines ->
            adapter.submitList(lines)
            binding.tvEmptyCart.isVisible = lines.isEmpty()
            binding.btnCheckout.isEnabled = lines.isNotEmpty()
        }
        viewModel.itemCount.observe(viewLifecycleOwner) { count ->
            binding.tvItems.text = getString(R.string.items_count, count)
        }
        viewModel.subtotal.observe(viewLifecycleOwner) { subtotal ->
            binding.tvTotal.text = getString(
                R.string.total_format,
                getString(R.string.total),
                CurrencyFormatter.format(requireContext(), subtotal)
            )
        }

        // Barcode coming back from the scanner screen
        setFragmentResultListener(ScannerContract.REQUEST_KEY) { _, bundle ->
            bundle.getString(ScannerContract.KEY_BARCODE)?.let { viewModel.onBarcodeScanned(it) }
        }

        binding.fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_billingFragment_to_scannerFragment)
        }
        setupSearch()
        binding.btnClear.setOnClickListener { viewModel.clearCart() }
        binding.btnCheckout.setOnClickListener { showCheckoutDialog() }

        // One-shot events from the ViewModel
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                null -> Unit

                is BillingEvent.OutOfStock -> {
                    toast(getString(R.string.not_enough_stock, event.productName))
                    viewModel.clearEvent()
                }

                is BillingEvent.UnknownBarcode -> {
                    viewModel.clearEvent()
                    showUnknownBarcodeDialog(event.barcode)
                }

                is BillingEvent.SaleCompleted -> {
                    viewModel.clearEvent()
                    findNavController().navigate(
                        R.id.action_billingFragment_to_receiptFragment,
                        bundleOf("saleId" to event.saleId)
                    )
                }
            }
        }
    }

    /**
     * Inline search (like a file manager / website search bar): type in the
     * bar itself, matching products appear right below it, tap to add to the
     * bill. Results stay open so several items can be added fast; clear the
     * text (or tap X) and they disappear.
     */
    private fun setupSearch() {
        var shown: List<Product> = emptyList()

        fun refresh(query: String) {
            val q = query.trim()
            binding.btnClearSearch.isVisible = q.isNotEmpty()
            if (q.isEmpty()) {
                binding.searchResultsCard.isVisible = false
                return
            }
            // Word-prefix matching: first-word matches shown first,
            // other-word matches next, barcode/category matches last
            shown = SearchMatcher.filter(allProducts, q)
            val ctx = requireContext()
            val rows = if (shown.isEmpty()) {
                listOf(getString(R.string.no_match, q))
            } else {
                shown.map {
                    "${it.name}  ·  ${CurrencyFormatter.format(ctx, it.sellPrice)}  ·  stock ${it.stockQty}"
                }
            }
            binding.listSearchResults.adapter =
                ArrayAdapter(ctx, android.R.layout.simple_list_item_1, rows)
            binding.searchResultsCard.isVisible = true
        }

        binding.etSearch.doAfterTextChanged { refresh(it?.toString().orEmpty()) }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.clearFocus()
        }

        binding.listSearchResults.setOnItemClickListener { _, _, position, _ ->
            val product = shown.getOrNull(position) ?: return@setOnItemClickListener
            if (!viewModel.tryAddToCart(product)) {
                toast(getString(R.string.not_enough_stock, product.name))
            } else {
                // Refresh so the stock counts stay correct after the add
                refresh(binding.etSearch.text?.toString().orEmpty())
            }
        }
    }

    private fun showUnknownBarcodeDialog(barcode: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.unknown_barcode)
            .setMessage(getString(R.string.unknown_barcode_msg, barcode))
            .setPositiveButton(R.string.add) { _, _ ->
                findNavController().navigate(
                    R.id.action_billingFragment_to_productFormFragment,
                    bundleOf("prefillBarcode" to barcode)
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCheckoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_checkout, null)
        val tvSubtotal = dialogView.findViewById<TextView>(R.id.tvCheckoutSubtotal)
        val etDiscount = dialogView.findViewById<TextInputEditText>(R.id.etDiscount)
        val chipCash = dialogView.findViewById<Chip>(R.id.chipCash)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvCheckoutTotal)

        val subtotal = viewModel.subtotal.value ?: 0.0
        tvSubtotal.text = getString(
            R.string.subtotal_format,
            getString(R.string.subtotal),
            CurrencyFormatter.format(requireContext(), subtotal)
        )
        tvTotal.text = getString(
            R.string.total_format,
            getString(R.string.total),
            CurrencyFormatter.format(requireContext(), subtotal)
        )

        // Live total preview while typing the discount
        etDiscount.doAfterTextChanged { text ->
            val discount = (text?.toString()?.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
            tvTotal.text = getString(
                R.string.total_format,
                getString(R.string.total),
                CurrencyFormatter.format(requireContext(), subtotal - discount)
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.checkout)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val discount = (etDiscount.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    .coerceIn(0.0, subtotal)
                val method = if (chipCash.isChecked) "CASH" else "CARD"
                viewModel.checkout(discount, method)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
