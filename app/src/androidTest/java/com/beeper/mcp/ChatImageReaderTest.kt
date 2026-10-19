package com.beeper.mcp

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatImageReaderTest {
    @Test fun validImageDecodesToJpeg() {
        val bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        val image = ChatImageDecoder.prepareInput(ByteArrayInputStream(output.toByteArray()))
        assertEquals(20, image.width)
        assertEquals(10, image.height)
        assertEquals(0xff, image.bytes[0].toInt() and 0xff)
        assertEquals(0xd8, image.bytes[1].toInt() and 0xff)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidImageRejected() {
        ChatImageDecoder.prepareInput(ByteArrayInputStream("not an image".toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedInputRejected() {
        ChatImageDecoder.prepareInput(ByteArrayInputStream(ByteArray(12 * 1024 * 1024 + 1)))
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
