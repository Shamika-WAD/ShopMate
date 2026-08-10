package com.shopmate.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.shopmate.app.R
import com.shopmate.app.databinding.FragmentSettingsBinding
import com.shopmate.app.printer.ThermalPrinter
import com.shopmate.app.util.BarcodeUtils
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.Settings
import com.shopmate.app.util.ThemeUtils
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private val printer by lazy { ThermalPrinter(requireContext()) }
    private val settings by lazy { Settings(requireContext()) }

    private var pendingBluetoothAction: (() -> Unit)? = null

    private val bluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val action = pendingBluetoothAction
            pendingBluetoothAction = null
            if (granted) action?.invoke() else toast(R.string.bt_permission)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // ---- Appearance (light / dark) ----
        binding.themeToggle.check(
            when (settings.themeMode) {
                1 -> R.id.btnThemeLight
                2 -> R.id.btnThemeDark
                else -> R.id.btnThemeSystem
            }
        )
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btnThemeLight -> 1
                R.id.btnThemeDark -> 2
                else -> 0
            }
            settings.themeMode = mode
            ThemeUtils.apply(mode) // switches instantly and recreates the screens
        }

        // ---- Bill text & Shop info ----
        setupBillPreview()
        binding.etShopName.setText(settings.shopName)
        binding.etAddress.setText(settings.address)
        binding.etPhone.setText(settings.phone)
        binding.etReceiptFooter.setText(settings.receiptFooter)

        // ---- Other preferences ----
        binding.etCurrency.setText(settings.currencySymbol)
        binding.etExpiryDays.setText(settings.expiryWarnDays.toString())
        binding.etDefaultThreshold.setText(settings.defaultLowStock.toString())

        binding.btnSaveSettings.setOnClickListener {
            settings.shopName = binding.etShopName.text.toString().trim()
            settings.address = binding.etAddress.text.toString().trim()
            settings.phone = binding.etPhone.text.toString().trim()
            settings.receiptFooter = binding.etReceiptFooter.text.toString().trim()
            
            settings.currencySymbol = binding.etCurrency.text.toString().trim()
            binding.etExpiryDays.text.toString().toIntOrNull()?.let { settings.expiryWarnDays = it }
            binding.etDefaultThreshold.text.toString().toIntOrNull()?.let { settings.defaultLowStock = it }

            setupBillPreview() // Refresh the preview immediately
            toast(R.string.settings_saved)
        }

        // ---- Bluetooth thermal printer ----
        refreshPrinterStatus()
        binding.btnSelectPrinter.setOnClickListener { withBluetooth { showPrinterPicker() } }
        binding.btnTestPrint.setOnClickListener { withBluetooth { testPrint() } }

        // ---- Data & demo ----
        binding.btnLoadDemo.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadDemo()
                toast(R.string.demo_loaded)
            }
        }
        binding.btnExportProducts.setOnClickListener {
            export { viewModel.exportProducts() }
        }
        binding.btnExportSales.setOnClickListener {
            export { viewModel.exportSales() }
        }
    }

    // ---------------- bill preview ----------------

    private fun setupBillPreview() {
        val bill = binding.root.findViewById<View>(R.id.billPreview)
        val currency = settings.currencySymbol
        val money = { v: Double -> CurrencyFormatter.format(requireContext(), v).replace(currency, "").trim() }
        
        // Populate header
        bill.findViewById<TextView>(R.id.tvShopName).text = settings.shopName
        bill.findViewById<TextView>(R.id.tvAddress).apply {
            isVisible = settings.address.isNotBlank()
            text = settings.address
        }
        bill.findViewById<TextView>(R.id.tvPhone).apply {
            isVisible = settings.phone.isNotBlank()
            text = settings.phone
        }
        bill.findViewById<TextView>(R.id.tvSaleId).text = 
            getString(R.string.receipt_number, 123)
        bill.findViewById<TextView>(R.id.tvBillDate).text = getString(R.string.sample_date)
        bill.findViewById<TextView>(R.id.tvHeaderAmount).text = getString(R.string.col_amount, currency)

        // Fill static dummy data to look like a real bill
        val container = bill.findViewById<ViewGroup>(R.id.containerItems)
        container.removeAllViews()
        val dummyItems = listOf(
            Triple("Milk Powder", 500.0, 1),
            Triple("White Bread", 120.0, 1)
        )
        dummyItems.forEach { (name, price, q) ->
            val row = layoutInflater.inflate(R.layout.item_receipt_line, container, false)
            row.findViewById<TextView>(R.id.tvItemName).text = name
            row.findViewById<TextView>(R.id.tvItemQty).text = q.toString()
            row.findViewById<TextView>(R.id.tvItemUnitPrice).text = money(price)
            row.findViewById<TextView>(R.id.tvItemAmount).text = money(price * q)
            container.addView(row)
        }

        val formatTotal = { v: Double -> CurrencyFormatter.format(requireContext(), v) }
        bill.findViewById<TextView>(R.id.tvSubtotal).text = formatTotal(620.0)
        bill.findViewById<TextView>(R.id.tvTotal).text = formatTotal(620.0)
        bill.findViewById<TextView>(R.id.tvPaymentMethod).text = "CASH"
        bill.findViewById<TextView>(R.id.tvBarcodeText).text = "SM000123"
        
        BarcodeUtils.makeBitmap("SM000123", BarcodeFormat.CODE_128, 720, 120)?.let {
            bill.findViewById<android.widget.ImageView>(R.id.ivBarcode).setImageBitmap(it)
        }

        bill.findViewById<TextView>(R.id.tvFooter).apply {
            isVisible = settings.receiptFooter.isNotBlank()
            text = settings.receiptFooter
        }
    }

    // ---------------- printer helpers ----------------

    private fun refreshPrinterStatus() {
        binding.tvPrinterStatus.text = settings.printerName
            ?.let { getString(R.string.printer_set, it) }
            ?: getString(R.string.printer_none)
    }

    private fun showPrinterPicker() {
        val devices = printer.pairedDevices()
        if (devices.isEmpty()) {
            toast(R.string.no_paired)
            return
        }
        val names = devices.map { "${it.name}\n${it.address}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_printer)
            .setItems(names) { _, which ->
                val device = devices[which]
                settings.printerAddress = device.address
                settings.printerName = device.name
                refreshPrinterStatus()
                toast(getString(R.string.printer_saved, device.name))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun testPrint() {
        viewLifecycleOwner.lifecycleScope.launch {
            toast(R.string.printing)
            handlePrintResult(
                printer.print(ThermalPrinter.TEST_TEXT + settings.shopName + "\n")
            )
        }
    }

    private fun handlePrintResult(result: ThermalPrinter.PrintResult) {
        when (result) {
            ThermalPrinter.PrintResult.Success -> toast(R.string.printed)
            ThermalPrinter.PrintResult.NoPrinterConfigured -> toast(R.string.printer_none)
            is ThermalPrinter.PrintResult.Error ->
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
        }
    }

    /** BLUETOOTH_CONNECT runtime permission is required from Android 12 onwards. */
    private fun withBluetooth(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            pendingBluetoothAction = action
            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            action()
        }
    }

    // ---------------- export helpers ----------------

    private fun export(action: suspend () -> File) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                shareCsv(action())
            } catch (e: Exception) {
            e.printStackTrace()
                toast(R.string.error_generic)
            }
        }
    }

    private fun shareCsv(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export_ready)))
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
