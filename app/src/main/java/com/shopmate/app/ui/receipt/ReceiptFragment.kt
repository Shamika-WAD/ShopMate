package com.shopmate.app.ui.receipt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.shopmate.app.R
import com.shopmate.app.databinding.FragmentReceiptBinding
import com.shopmate.app.printer.ThermalPrinter
import com.shopmate.app.util.CurrencyFormatter
import com.shopmate.app.util.DateUtils
import com.shopmate.app.util.Settings
import com.shopmate.app.util.BarcodeUtils
import com.shopmate.app.util.ViewUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Bill preview shown right after checkout (and from Sales history):
 * a designed paper-like receipt card with Print / Share / Done buttons.
 * Printing still uses the plain-text receipt from ReceiptBuilder, 
 * while Sharing captures the designed card as a photo.
 */
class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReceiptViewModel by viewModels()
    private val printer by lazy { ThermalPrinter(requireContext()) }
    private val settings by lazy { Settings(requireContext()) }

    private var receiptText: String? = null
    private var pendingBluetoothAction: (() -> Unit)? = null

    private val bluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val action = pendingBluetoothAction
            pendingBluetoothAction = null
            if (granted) action?.invoke() else toast(getString(R.string.bt_permission))
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val saleId = arguments?.getLong("saleId", -1L) ?: -1L
        viewModel.load(saleId)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state == null) {
                toast(getString(R.string.sale_open_error))
                findNavController().popBackStack()
                return@observe
            }
            receiptText = state.receiptText
            fillReceipt(state)
        }

        binding.btnPrintReceipt.setOnClickListener { withBluetooth { printReceipt() } }
        binding.btnShareReceipt.setOnClickListener { shareReceipt() }
        binding.btnDoneReceipt.setOnClickListener { findNavController().popBackStack() }
    }

    /** Fills the designed receipt card with the sale's data. */
    /** Fills the bill (bill_preview.xml) with the sale's data. */
    private fun fillReceipt(state: ReceiptViewModel.ReceiptState) {
        val sale = state.sale
        val currency = settings.currencySymbol
        val money = { value: Double -> CurrencyFormatter.format(requireContext(), value).replace(currency, "").trim() }
        val bill = binding.root.findViewById<View>(R.id.billPreview)

        // Header (the "permanent" texts from Settings)
        bill.findViewById<TextView>(R.id.tvShopName).text = settings.shopName
        bill.findViewById<TextView>(R.id.tvAddress).apply {
            isVisible = settings.address.isNotBlank()
            text = settings.address
        }
        bill.findViewById<TextView>(R.id.tvPhone).apply {
            isVisible = settings.phone.isNotBlank()
            text = settings.phone
        }
        bill.findViewById<TextView>(R.id.tvSaleId).text = getString(R.string.receipt_number, sale.id)
        bill.findViewById<TextView>(R.id.tvBillDate).text = DateUtils.formatDateTime(sale.timestamp)
        bill.findViewById<TextView>(R.id.tvHeaderAmount).text = getString(R.string.col_amount, currency)

        // One 4-column row (Name | Qty | Unit Price | Amount) per sold item
        val container = bill.findViewById<ViewGroup>(R.id.containerItems)
        container.removeAllViews()
        state.items.forEach { item ->
            val row = layoutInflater.inflate(
                R.layout.item_receipt_line, container, false
            )
            row.findViewById<TextView>(R.id.tvItemName).text = item.productName
            row.findViewById<TextView>(R.id.tvItemQty).text = item.qty.toString()
            row.findViewById<TextView>(R.id.tvItemUnitPrice).text = money(item.unitPrice)
            row.findViewById<TextView>(R.id.tvItemAmount).text = money(item.lineTotal)
            container.addView(row)
        }

        // Totals
        val formatTotal = { value: Double -> CurrencyFormatter.format(requireContext(), value) }
        bill.findViewById<TextView>(R.id.tvSubtotal).text = formatTotal(sale.subtotal)
        bill.findViewById<View>(R.id.rowDiscount).isVisible = sale.discount > 0
        bill.findViewById<TextView>(R.id.tvDiscount).text = getString(R.string.discount_format, formatTotal(sale.discount))
        bill.findViewById<TextView>(R.id.tvTotal).text = formatTotal(sale.total)
        bill.findViewById<TextView>(R.id.tvPaymentMethod).text = sale.paymentMethod

        // Real scannable barcode of the receipt number (like supermarket bills)
        val code = "SM" + "%06d".format(sale.id)
        BarcodeUtils.makeBitmap(code, BarcodeFormat.CODE_128, 720, 120)
            ?.let { bill.findViewById<android.widget.ImageView>(R.id.ivBarcode).setImageBitmap(it) }
        bill.findViewById<TextView>(R.id.tvBarcodeText).text = code

        // Thank-you footer (edited in Settings, may be several lines)
        bill.findViewById<TextView>(R.id.tvFooter).apply {
            isVisible = settings.receiptFooter.isNotBlank()
            text = settings.receiptFooter
        }
    }

    private fun printReceipt() {
        val text = receiptText ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            toast(getString(R.string.printing))
            when (val result = printer.print(text)) {
                ThermalPrinter.PrintResult.Success -> toast(getString(R.string.printed))
                ThermalPrinter.PrintResult.NoPrinterConfigured -> showPrinterSetupDialog()
                is ThermalPrinter.PrintResult.Error -> toast(result.message)
            }
        }
    }

    private fun showPrinterSetupDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.printer_header)
            .setMessage(R.string.printer_none)
            .setPositiveButton(R.string.settings) { _, _ ->
                findNavController().navigate(R.id.action_receiptFragment_to_settingsFragment)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun shareReceipt() {
        val saleId = viewModel.state.value?.sale?.id ?: return
        val billView = binding.root.findViewById<View>(R.id.billPreview)
        val bitmap = ViewUtils.capture(billView)

        try {
            val cachePath = File(requireContext().cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "receipt_$saleId.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            toast(getString(R.string.error_generic))
            e.printStackTrace()
        }
    }

    /** BLUETOOTH_CONNECT is only needed from Android 12 (API 31) onwards. */
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

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
