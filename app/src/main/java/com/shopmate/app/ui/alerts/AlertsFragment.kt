package com.shopmate.app.ui.alerts

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
import com.shopmate.app.databinding.FragmentAlertsBinding

/**
 * Two warning lists:
 *  - Low stock (quantity at/below the product's alert level)
 *  - Expiring soon (expiry date within the days configured in Settings)
 * Tapping a row opens the product so it can be restocked / fixed.
 */
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lowAdapter = AlertAdapter(AlertMode.LOW_STOCK) { openProduct(it.id) }
        val expAdapter = AlertAdapter(AlertMode.EXPIRING) { openProduct(it.id) }

        binding.recyclerLow.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLow.adapter = lowAdapter
        binding.recyclerExp.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExp.adapter = expAdapter

        viewModel.lowStock.observe(viewLifecycleOwner) { list ->
            lowAdapter.submitList(list)
            binding.tvLowEmpty.isVisible = list.isEmpty()
        }
        viewModel.expiring.observe(viewLifecycleOwner) { list ->
            expAdapter.submitList(list)
            binding.tvExpEmpty.isVisible = list.isEmpty()
        }
    }

    private fun openProduct(productId: Long) {
        findNavController().navigate(
            R.id.action_alertsFragment_to_productFormFragment,
            bundleOf("productId" to productId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
