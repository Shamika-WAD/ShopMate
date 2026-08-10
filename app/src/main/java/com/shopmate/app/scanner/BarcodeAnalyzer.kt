package com.shopmate.app.scanner

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis use-case: feeds each camera frame to ML Kit and
 * reports the decoded barcode string through [onBarcode].
 *
 * The same barcode is ignored for 1.5 seconds after it is first seen,
 * so one scan does not fire the callback dozens of times.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
class BarcodeAnalyzer(
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    private var lastValue: String? = null
    private var lastTime: Long = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                val now = SystemClock.elapsedRealtime()
                if (!value.isNullOrBlank() && ((value != lastValue) || ((now - lastTime) > 1500))) {
                    lastValue = value
                    lastTime = now
                    onBarcode(value)
                }
            }
            .addOnCompleteListener {
                // ALWAYS close the frame, or the camera feed freezes.
                imageProxy.close()
            }
    }
}
