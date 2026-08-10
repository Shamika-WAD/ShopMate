package com.shopmate.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.createBitmap

object ViewUtils {

    /** Captures a View's current visual state into a Bitmap. */
    fun capture(view: View): Bitmap {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}
