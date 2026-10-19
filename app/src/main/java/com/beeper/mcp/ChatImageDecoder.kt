package com.beeper.mcp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded image decoder retained for the currently disabled chat image reader. No inbox or cache. */
object ChatImageDecoder {
    data class Image(val bytes: ByteArray, val width: Int, val height: Int)
    private const val MAX_INPUT = 12 * 1024 * 1024
    private const val MAX_OUTPUT = 5 * 1024 * 1024

    fun prepareInput(input: InputStream): Image {
        val raw = run {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                require(output.size() + n <= MAX_INPUT) { "Image exceeds 12 MB; crop it first." }
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This file is not a supported image." }
        require(bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000L) {
            "Image dimensions are too large; crop it first."
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3200) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Could not decode the image.")
        return try {
            val output = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "Image conversion failed." }
            require(output.size() <= MAX_OUTPUT) { "Image exceeds 5 MB after conversion; crop it first." }
            Image(output.toByteArray(), bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

}
