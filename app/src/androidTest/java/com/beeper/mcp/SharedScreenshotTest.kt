package com.beeper.mcp

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import io.modelcontextprotocol.kotlin.sdk.TextContent
import java.io.ByteArrayOutputStream
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedScreenshotTest {
    @After fun cleanup() { SharedScreenshot.clear() }

    @Test fun noScreenshotReturnsOnlyGuidance() {
        SharedScreenshot.clear()
        val result = SharedScreenshot.result()
        assertTrue(result.content.single() is TextContent)
    }

    @Test fun approvedImageReturnsRealImageAndCanBeCleared() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        bitmap.recycle()
        val bytes = output.toByteArray()
        SharedScreenshot.approve(SharedScreenshot.Image(bytes, 2, 2, 0))
        val result = SharedScreenshot.result()
        val image = result.content.filterIsInstance<ImageContent>().single()
        assertEquals("image/jpeg", image.mimeType)
        assertArrayEquals(bytes, Base64.decode(image.data, Base64.NO_WRAP))
        SharedScreenshot.clear()
        assertNull(SharedScreenshot.get())
    }
}
