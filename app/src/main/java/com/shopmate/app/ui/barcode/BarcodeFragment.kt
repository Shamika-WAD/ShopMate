package com.shopmate.app.ui.barcode

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.shopmate.app.R
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.databinding.FragmentBarcodeBinding
import com.shopmate.app.util.BarcodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows the barcode image for a product so it can be shared as a PNG
 * and printed onto stickers — for items that come without a barcode.
 */
class BarcodeFragment : Fragment() {

    private var _binding: FragmentBarcodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BarcodeViewModel by viewModels()
    private var product: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val productId = arguments?.getLong("productId", -1L) ?: -1L
        viewModel.load(productId)

        viewModel.product.observe(viewLifecycleOwner) { p ->
            if (p == null) {
                toast(getString(R.string.error_generic))
                findNavController().popBackStack()
                return@observe
            }
            product = p
            binding.tvProductName.text = p.name
            binding.tvBarcodeValue.text = p.barcode
            setupFormatChips(p.barcode)
            renderBarcode()
        }

        binding.chipFormatGroup.setOnCheckedStateChangeListener { _, _ -> renderBarcode() }
        binding.btnShareBarcode.setOnClickListener { shareBarcode() }
    }

    /** EAN-13 only accepts 12-13 digit numbers; anything else forces Code 128. */
    private fun setupFormatChips(code: String) {
        val eanOk = BarcodeUtils.isEan13Compatible(code)
        binding.chipEan13.isEnabled = eanOk
        if (eanOk) binding.chipEan13.isChecked = true
        else binding.chipCode128.isChecked = true
    }

    private fun currentFormat(): BarcodeFormat =
        if (binding.chipEan13.isChecked && binding.chipEan13.isEnabled) {
            BarcodeFormat.EAN_13
        } else {
            BarcodeFormat.CODE_128
        }

    private fun renderBarcode() {
        val p = product ?: return
        // Preview shows the exact same print-ready label that will be shared
        val bitmap: Bitmap? = BarcodeUtils.makeLabelBitmap(p.name, p.barcode, currentFormat())
        binding.imgBarcode.setImageBitmap(bitmap)
        binding.tvBarcodeError.isVisible = bitmap == null
    }

    private fun shareBarcode() {
        val p = product ?: return
        val bitmap = BarcodeUtils.makeLabelBitmap(p.name, p.barcode, currentFormat()) ?: run {
            toast(getString(R.string.error_generic))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                BarcodeUtils.savePng(requireContext(), bitmap, "barcode_${p.barcode}")
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${p.name} — ${p.barcode}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_barcode)))
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
