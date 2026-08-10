package com.shopmate.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream

/**
 * Generates barcode images (for products that come without a barcode)
 * using the ZXing library — same engine used by most scanner apps.
 *
 * Codes this app auto-generates start with "20" + 10 digits = 12 digits,
 * which is the industry-reserved "in-store" EAN-13 range, so they never
 * collide with real manufacturer barcodes.
 */
object BarcodeUtils {

    /** EAN-13 needs exactly 12 or 13 digits, numbers only. */
    fun isEan13Compatible(code: String): Boolean =
        code.matches(Regex("\\d{12,13}"))

    /** Renders raw scanner bars to a Bitmap (null if the format can't encode it). */
    fun makeBitmap(
        content: String,
        format: BarcodeFormat,
        width: Int = 880,
        height: Int = 280
    ): Bitmap? {
        return try {
            val matrix = MultiFormatWriter().encode(content, format, width, height)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            var i = 0
            for (y in 0 until h) {
                for (x in 0 until w) {
                    pixels[i++] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a print-ready label on a white background:
     *
     *      ┌──────────────────────────┐
     *      │       Product name       │
     *      │   ▌▍█▌▍▌█▍█▌▌█▍█▌▍▌█▍   │   <- the scannable bars
     *      │      2 0 1 2 3 4 5 6     │   <- code printed under bars
     *      └──────────────────────────┘
     *
     * What you see in the app is exactly what prints — no editing needed.
     */
    fun makeLabelBitmap(
        name: String,
        code: String,
        format: BarcodeFormat,
        width: Int = 900
    ): Bitmap? {
        val margin = 48
        val bars = makeBitmap(code, format, width - margin * 2, 220) ?: return null

        // Product name (auto-shrinks until it fits the label width)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        var nameSize = 54f
        namePaint.textSize = nameSize
        while (namePaint.measureText(name) > width - margin * 2 && nameSize > 20f) {
            nameSize -= 4f
            namePaint.textSize = nameSize
        }

        // Code digits printed under the bars
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 46f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val nameHeight = (namePaint.fontMetrics.descent - namePaint.fontMetrics.ascent).toInt()
        val codeHeight = (codePaint.fontMetrics.descent - codePaint.fontMetrics.ascent).toInt()

        val height = margin + nameHeight + 26 + bars.height + 20 + codeHeight + margin
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val centerX = width / 2f

        // name
        var baseline = margin - namePaint.fontMetrics.ascent
        canvas.drawText(name, centerX, baseline, namePaint)

        // bars
        val barsTop = (margin + nameHeight + 26).toFloat()
        canvas.drawBitmap(bars, margin.toFloat(), barsTop, null)

        // code under bars
        baseline = barsTop + bars.height + 20 - codePaint.fontMetrics.ascent
        canvas.drawText(code, centerX, baseline, codePaint)

        return out
    }

    /** Saves a PNG into the shared cache folder (FileProvider can share it). */
    fun savePng(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
