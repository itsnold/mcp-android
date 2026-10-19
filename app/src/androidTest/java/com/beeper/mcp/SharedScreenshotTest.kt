package com.beeper.mcp

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import io.modelcontextprotocol.kotlin.sdk.TextContent
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
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

    @Test fun chatImageDecodingDoesNotApproveOrCacheTheImage() {
        SharedScreenshot.clear()
        val bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        val image = SharedScreenshot.prepareInput(ByteArrayInputStream(output.toByteArray()))
        assertEquals(20, image.width)
        assertEquals(10, image.height)
        assertEquals(0xff, image.bytes[0].toInt() and 0xff)
        assertEquals(0xd8, image.bytes[1].toInt() and 0xff)
        assertNull(SharedScreenshot.get())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidImageRejected() {
        SharedScreenshot.prepareInput(ByteArrayInputStream("not an image".toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedInputRejected() {
        SharedScreenshot.prepareInput(ByteArrayInputStream(ByteArray(12 * 1024 * 1024 + 1)))
    }

    @Test fun locationDiagnosticsDoNotRevealPrivatePaths() {
        assertEquals("absolute file path", describeChatImageLocation("/data/user/0/com.beeper.android/private-name.jpg"))
        assertEquals("HTTP URL (not downloaded)", describeChatImageLocation("https://example.com/private-name.jpg?token=secret"))
        assertEquals("content URI (authority: com.beeper.api)", describeChatImageLocation("content://com.beeper.api/private-name.jpg?token=secret"))
        assertEquals("content URI (authority: com.beeper.api)", describeChatImageLocation("content://user:secret@com.beeper.api/private-name.jpg?token=secret"))
        assertEquals("unsupported URI scheme: mxc", describeChatImageLocation("mxc://example.com/private-id?token=secret"))
        assertEquals("unsupported URI scheme: custom+media", describeChatImageLocation("custom+media://private-id"))
    }
}
