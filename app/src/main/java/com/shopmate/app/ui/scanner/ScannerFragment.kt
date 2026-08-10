package com.shopmate.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.shopmate.app.R
import com.shopmate.app.databinding.FragmentScannerBinding
import com.shopmate.app.scanner.BarcodeAnalyzer
import com.shopmate.app.util.ScannerContract
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen camera that scans a barcode and returns the value to the
 * previous screen (Billing or Product form) via the Fragment Result API.
 */
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Ensures only the first detected barcode is delivered. */
    private val handled = AtomicBoolean(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.camera_permission_denied,
                    Toast.LENGTH_LONG,
                ).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnCancelScan.setOnClickListener { findNavController().popBackStack() }

        handled.set(false)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener(
            {
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build()
            val currentBinding = _binding ?: return@addListener
            preview.setSurfaceProvider(currentBinding.previewView.surfaceProvider)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor, BarcodeAnalyzer(::onBarcodeFound))

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
            e.printStackTrace()
                Toast.makeText(requireContext(), R.string.camera_error, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun onBarcodeFound(code: String) {
        // Analyzer runs on a background thread -> hop to the main thread.
        if (!handled.compareAndSet(false, true)) return
        mainHandler.post {
            if (!isAdded) return@post
            setFragmentResult(
                ScannerContract.REQUEST_KEY,
                bundleOf(ScannerContract.KEY_BARCODE to code)
            )
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        analysisExecutor.shutdown()
        super.onDestroy()
    }
}
