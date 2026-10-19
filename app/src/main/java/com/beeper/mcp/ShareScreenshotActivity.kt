package com.beeper.mcp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.beeper.mcp.ui.theme.BeeperMcpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareScreenshotActivity : ComponentActivity() {
    private var status by mutableStateOf("Share one screenshot from Gallery, Messenger, or Beeper to this app.")
    private var preview by mutableStateOf<Bitmap?>(null)
    private var pending by mutableStateOf<SharedScreenshot.Image?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeeperMcpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Screenshot for OpenCode", style = MaterialTheme.typography.headlineSmall)
                        Text(status)
                        preview?.let { Image(it.asImageBitmap(), "Shared screenshot preview",
                            Modifier.fillMaxWidth().weight(1f)) }
                        if (pending != null) {
                            Text("Only share images you want the assistant to see. Internet-enabled apps on this phone may also reach the unauthenticated localhost MCP server. The image stays in memory for up to 30 minutes; an AI client may retain its own copy after retrieval.")
                            Button(onClick = {
                                pending?.let { SharedScreenshot.approve(it) }
                                pending = null
                                preview = null
                                status = "Ready. Keep the MCP server running, return to OpenCode, and say: Read my shared screenshot. Sharing does not send a message to OpenCode automatically."
                            }) { Text("Make available to OpenCode") }
                        }
                        OutlinedButton(onClick = {
                            SharedScreenshot.clear()
                            pending = null
                            preview = null
                            status = "Screenshot removed from this app's memory. Copies already retrieved by clients are not removed."
                        }) { Text("Clear screenshot") }
                        OutlinedButton(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
        if (intent.action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri == null || intent.type?.startsWith("image/") != true) {
                status = "No image found. Use Share on a single screenshot."
                return
            }
            status = "Preparing image locally..."
            lifecycleScope.launch {
                try {
                    val image = withContext(Dispatchers.IO) { SharedScreenshot.prepare(contentResolver, uri) }
                    preview = withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                    }
                    pending = image
                    status = "Preview: ${image.width} x ${image.height}. Approve below to expose this image through MCP."
                } catch (e: Exception) {
                    status = "Could not prepare screenshot: ${e.message}"
                }
            }
        }
    }
}
