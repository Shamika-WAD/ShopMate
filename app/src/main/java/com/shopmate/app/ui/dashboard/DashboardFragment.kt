package com.shopmate.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.shopmate.app.MainActivity
import com.shopmate.app.R
import com.shopmate.app.databinding.FragmentDashboardBinding
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.DateUtils

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.tvGreeting.text = getString(R.string.welcome_shop, viewModel.shopName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDate.text = DateUtils.formatDate(System.currentTimeMillis())

        viewModel.todaySales.observe(viewLifecycleOwner) {
            binding.tvTodaySales.text = CurrencyFormatter.format(requireContext(), it)
        }
        viewModel.todayBills.observe(viewLifecycleOwner) {
            binding.tvTodayBills.text = it.toString()
        }
        viewModel.lowStockCount.observe(viewLifecycleOwner) {
            binding.tvLowStockCount.text = it.toString()
        }
        viewModel.expiringCount.observe(viewLifecycleOwner) {
            binding.tvExpiringCount.text = it.toString()
        }

        // Tapping a warning card opens the Alerts tab
        binding.cardLowStock.setOnClickListener {
            (activity as? MainActivity)?.openTab(R.id.alertsFragment)
        }
        binding.cardExpiring.setOnClickListener {
            (activity as? MainActivity)?.openTab(R.id.alertsFragment)
        }

        // Quick actions (tab destinations switched like a real tab tap)
        binding.btnNewSale.setOnClickListener {
            (activity as? MainActivity)?.openTab(R.id.billingFragment)
        }
        binding.btnAddProduct.setOnClickListener {
            // Open "new product" THROUGH the Inventory tab: switch to the
            // Inventory list first, then push the form on top of it.
            (activity as? MainActivity)?.openTab(R.id.inventoryFragment)
            findNavController().navigate(R.id.action_inventoryFragment_to_productFormFragment)
        }
        binding.btnViewSales.setOnClickListener {
            (activity as? MainActivity)?.openTab(R.id.salesFragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
