package com.shopmate.app.ui.inventory

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.FragmentProductFormBinding
import com.shopmate.app.util.DateUtils
import com.shopmate.app.util.ScannerContract
import com.shopmate.app.util.Settings
import java.util.Calendar

/**
 * Add or edit a product.
 * - productId = -1  -> new product (defaults pre-filled)
 * - prefillBarcode  -> used when coming from "unknown barcode" in billing
 */
class ProductFormFragment : Fragment() {

    private var _binding: FragmentProductFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductFormViewModel by viewModels()

    private var editingProduct: Product? = null
    private var selectedExpiry: Long? = null
    private var pendingBarcode: String? = null
    private var populated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            populated = it.getBoolean(KEY_POPULATED, false)
            selectedExpiry = if (it.containsKey(KEY_EXPIRY)) it.getLong(KEY_EXPIRY) else null
            pendingBarcode = it.getString(KEY_PENDING_BARCODE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_POPULATED, populated)
        selectedExpiry?.let { outState.putLong(KEY_EXPIRY, it) }
        pendingBarcode?.let { outState.putString(KEY_PENDING_BARCODE, it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val productId = arguments?.getLong("productId", -1L) ?: -1L

        // Defaults for a brand-new product
        if (!populated && (productId == -1L)) {
            binding.etUnit.setText(R.string.unit_pcs)
            binding.etThreshold.setText(Settings(requireContext()).defaultLowStock.toString())
            arguments?.getString("prefillBarcode")?.let { binding.etBarcode.setText(it) }
            populated = true
        }

        // Editing: load and fill the fields once
        if (productId != -1L) {
            viewModel.load(productId)
            viewModel.product.observe(viewLifecycleOwner) { product ->
                if (product != null && editingProduct == null) {
                    populate(product)
                }
            }
        }

        renderExpiry()
        pendingBarcode?.let {
            binding.etBarcode.setText(it)
            pendingBarcode = null
        }

        // The Scanner screen returns its value through the Fragment Result API.
        // Registered against the fragment (not the view) so it survives the
        // trip to the scanner and back.
        setFragmentResultListener(ScannerContract.REQUEST_KEY) { _, bundle ->
            bundle.getString(ScannerContract.KEY_BARCODE)?.let { code ->
                val b = _binding
                if (b != null) b.etBarcode.setText(code) else pendingBarcode = code
            }
        }

        binding.btnScanBarcode.setOnClickListener {
            findNavController().navigate(R.id.action_productFormFragment_to_scannerFragment)
        }
        binding.btnExpiry.setOnClickListener { showExpiryPicker() }
        binding.btnSave.setOnClickListener { save() }

        // For products that come without a barcode: make a unique in-store code
        binding.btnGenerateCode.setOnClickListener {
            viewModel.generateUniqueCode { code ->
                binding.etBarcode.setText(code)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.generated_code, code),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Jump to the barcode image screen (only for an already-saved product)
        binding.btnViewBarcode.setOnClickListener {
            editingProduct?.let { product ->
                findNavController().navigate(
                    R.id.action_productFormFragment_to_barcodeFragment,
                    bundleOf("productId" to product.id)
                )
            }
        }
    }

    private fun populate(product: Product) {
        editingProduct = product
        binding.etName.setText(product.name)
        binding.etBarcode.setText(product.barcode)
        binding.etCategory.setText(product.category)
        binding.etUnit.setText(product.unit)
        binding.etCost.setText(product.costPrice?.toString() ?: "")
        binding.etPrice.setText(product.sellPrice.toString())
        binding.etStock.setText(product.stockQty.toString())
        binding.etThreshold.setText(product.lowStockThreshold.toString())
        selectedExpiry = product.expiryDate
        binding.btnViewBarcode.isVisible = product.barcode.isNotBlank()
        renderExpiry()
    }

    private fun showExpiryPicker() {
        val cal = Calendar.getInstance()
        selectedExpiry?.let { cal.timeInMillis = it }

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedExpiry = picked.timeInMillis
                renderExpiry()
            },
            cal[Calendar.YEAR],
            cal[Calendar.MONTH],
            cal[Calendar.DAY_OF_MONTH]
        )
        dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.clear)) { _, _ ->
            selectedExpiry = null
            renderExpiry()
        }
        dialog.show()
    }

    private fun renderExpiry() {
        binding.btnExpiry.text = selectedExpiry?.let {
            getString(R.string.expiry_set, DateUtils.formatDate(it))
        } ?: getString(R.string.expiry_none)
    }

    private fun save() {
        binding.tilName.error = null
        binding.tilBarcode.error = null
        binding.tilPrice.error = null

        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) {
            binding.tilName.error = getString(R.string.required)
            return
        }
        val barcode = binding.etBarcode.text.toString().trim()
        if (barcode.isBlank()) {
            binding.tilBarcode.error = getString(R.string.required)
            return
        }
        val price = binding.etPrice.text.toString().toDoubleOrNull()
        if (price == null) {
            binding.tilPrice.error = getString(R.string.required)
            return
        }

        val settings = Settings(requireContext())
        val product = Product(
            id = editingProduct?.id ?: 0,
            barcode = barcode,
            name = name,
            category = binding.etCategory.text.toString().trim(),
            unit = binding.etUnit.text.toString().trim().ifBlank { "pcs" },
            costPrice = binding.etCost.text.toString().toDoubleOrNull(),
            sellPrice = price,
            stockQty = binding.etStock.text.toString().toIntOrNull() ?: 0,
            lowStockThreshold = binding.etThreshold.text.toString().toIntOrNull()
                ?: settings.defaultLowStock,
            expiryDate = selectedExpiry,
            createdAt = editingProduct?.createdAt ?: System.currentTimeMillis()
        )

        viewModel.save(product) { success, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            if (success) findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val KEY_POPULATED = "populated"
        const val KEY_EXPIRY = "expiry"
        const val KEY_PENDING_BARCODE = "pendingBarcode"
    }
}
