package com.shopmate.app.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopmate.app.R
import com.shopmate.app.databinding.FragmentSalesBinding

/**
 * List of completed bills, newest first.
 * Tap one to open the bill preview (print / share it again).
 */
class SalesHistoryFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SalesAdapter { sale ->
            findNavController().navigate(
                R.id.action_salesFragment_to_receiptFragment,
                bundleOf("saleId" to sale.id)
            )
        }
        binding.recyclerSales.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSales.adapter = adapter

        viewModel.sales.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptySales.isVisible = list.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
