package com.frost.keyboard

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

object WallpaperBlurHelper {

    fun getBlurredWallpaper(context: Context, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
        return try {
            val wm = WallpaperManager.getInstance(context)
            val drawable: Drawable = wm.drawable ?: return null
            val full = drawableToBitmap(drawable)
            downscaleUpscaleBlur(full, targetWidth, targetHeight)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bmp
    }

    private fun downscaleUpscaleBlur(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val smallW = (targetWidth / 24).coerceAtLeast(2)
        val smallH = (targetHeight / 24).coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, targetWidth, targetHeight, true)
    }
}
