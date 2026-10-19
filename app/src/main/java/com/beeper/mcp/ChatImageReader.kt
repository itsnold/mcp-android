package com.beeper.mcp

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import java.io.File

/** Report the location format, never the private path or URL query. */
fun describeChatImageLocation(path: String): String {
    val uri = Uri.parse(path)
    return when (uri.scheme) {
        "content" -> "content URI (authority: ${uri.authority?.take(100)})"
        "file" -> "file URI"
        null -> if (File(path).isAbsolute) "absolute file path" else "relative path"
        "https", "http" -> "HTTP URL (not downloaded)"
        else -> "unsupported URI scheme"
    }
}

/** Read only a media location supplied by Beeper's own message provider. */
fun readChatImage(resolver: ContentResolver, path: String): ImageContent {
    val uri = Uri.parse(path)
    val image = when (uri.scheme) {
        "content" -> {
            require(uri.authority == "com.beeper.api" ||
                uri.authority?.startsWith("com.beeper.android.") == true) {
                "Beeper returned a content URI outside its known providers; not opened."
            }
            resolver.openInputStream(uri)?.use { SharedScreenshot.prepareInput(it) }
                ?: error("Beeper media provider returned no image stream.")
        }
        null, "file" -> {
            val file = File(if (uri.scheme == "file") uri.path ?: "" else path).canonicalFile
            val permitted = listOf(
                "/data/user/0/com.beeper.android/", "/data/data/com.beeper.android/",
                "/storage/emulated/0/Android/data/com.beeper.android/",
                "/storage/emulated/0/Android/media/com.beeper.android/"
            )
            require(file.isAbsolute && permitted.any { file.path.startsWith(it) }) {
                "Beeper returned a local image path outside its expected storage; not opened."
            }
            file.inputStream().use { SharedScreenshot.prepareInput(it) }
        }
        else -> error("Beeper returned a ${uri.scheme} media location, not an accessible Android image URI. No external download attempted.")
    }
    return ImageContent(Base64.encodeToString(image.bytes, Base64.NO_WRAP), "image/jpeg")
}
