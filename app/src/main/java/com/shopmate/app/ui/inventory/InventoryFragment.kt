package com.shopmate.app.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.FragmentInventoryBinding

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = InventoryAdapter(
            onEdit = { product -> openProductForm(product.id) },
            onDelete = { product -> confirmDelete(product) },
            onStockPlus = { product -> viewModel.adjustStock(product, +1) },
            onStockMinus = { product -> viewModel.adjustStock(product, -1) }
        )
        binding.recyclerInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInventory.adapter = adapter

        viewModel.products.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
        }

        binding.etSearch.doAfterTextChanged { text ->
            binding.btnClearSearch.isVisible = !text.isNullOrEmpty()
            viewModel.setQuery(text?.toString().orEmpty())
        }
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.clearFocus()
        }

        binding.fabAdd.setOnClickListener { openProductForm(-1L) }
    }

    private fun openProductForm(productId: Long) {
        findNavController().navigate(
            R.id.action_inventoryFragment_to_productFormFragment,
            bundleOf("productId" to productId)
        )
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_msg, product.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(product)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
