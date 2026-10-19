package com.beeper.mcp

import android.content.Context
import android.database.Cursor
import androidx.core.net.toUri
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.jsonPrimitive
import android.net.Uri

/** Inspect the real provider contract without exporting message contents or blobs. */
fun inspectMessageProvider(context: Context): CallToolResult {
    return try {
        @Suppress("DEPRECATION")
        val version = context.packageManager.getPackageInfo("com.beeper.android", 0).versionName
        val cursor = context.contentResolver.query(
            "content://com.beeper.api/messages?limit=5&offset=0".toUri(),
            null, null, null, null
        ) ?: error("Provider returned no cursor; cannot inspect its columns.")
        val report = cursor.use {
            val rows = mutableListOf<String>()
            while (it.moveToNext()) {
                rows.add(it.columnNames.mapIndexed { index, name ->
                    val kind = when (it.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> "null"
                        Cursor.FIELD_TYPE_INTEGER -> "integer"
                        Cursor.FIELD_TYPE_FLOAT -> "float"
                        Cursor.FIELD_TYPE_BLOB -> "blob (not read)"
                        else -> "string (value not read)"
                    }
                    "$name: $kind"
                }.joinToString("; "))
            }
            "Beeper Android version: $version\n" +
                "Actual message columns: ${it.columnNames.joinToString()}\n" +
                "Sampled rows: ${rows.size} (maximum 5). Only names and types; no message values.\n" +
                rows.mapIndexed { i, row -> "Row ${i + 1}: $row" }.joinToString("\n") +
                "\nThis checks returned columns only. It does not prove whether undocumented provider endpoints exist."
        }
        CallToolResult(content = listOf(TextContent(text = report)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent(text = "Provider inspection failed: ${e.javaClass.simpleName}: ${e.message}")), isError = true)
    }
}

/** Inspect only selected chats' media metadata, without fetching media or returning paths. */
fun inspectChatMedia(context: Context, request: CallToolRequest): CallToolResult {
    return try {
        val roomIds = request.arguments["roomIds"]?.jsonPrimitive?.content
        require(!roomIds.isNullOrBlank()) { "Select roomIds; global media inspection is not allowed." }
        val limit = (request.arguments["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 40).coerceIn(1, 100)
        val offset = (request.arguments["offset"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0).coerceAtLeast(0)
        val uri = "content://$BEEPER_AUTHORITY/messages".toUri().buildUpon()
            .appendQueryParameter("roomIds", roomIds)
            .appendQueryParameter("limit", limit.toString())
            .appendQueryParameter("offset", offset.toString()).build()
        val cursor = context.contentResolver.query(uri, null, null, null, null)
            ?: error("Beeper returned no cursor.")
        val report = cursor.use {
            val rows = mutableListOf<String>()
            val image = it.getColumnIndex("image_path")
            val video = it.getColumnIndex("video_path")
            val deleted = it.getColumnIndex("isDeleted")
            var sampled = 0
            while (sampled < limit && it.moveToNext()) {
                sampled++
                if (deleted >= 0 && it.getInt(deleted) == 1) continue
                listOf("image" to image, "video" to video).forEach { (kind, index) ->
                    if (index >= 0 && !it.isNull(index)) {
                        val path = it.getString(index)
                        if (!path.isNullOrBlank()) {
                            val mediaUri = Uri.parse(path)
                            // Host excludes user-info, path, query, and fragment; only network/content URIs have a relevant host.
                            val host = if (mediaUri.scheme in listOf("content", "mxc", "http", "https"))
                                mediaUri.host?.takeIf { host -> host.matches(Regex("[A-Za-z0-9.:-]{1,253}")) }
                            else null
                            rows.add("Message position ${offset + sampled}, $kind: ${describeChatImageLocation(path)}" +
                                (host?.let { host -> "; host: $host" } ?: ""))
                        }
                    }
                }
            }
            "Selected chat media metadata: sampled $sampled messages at offset $offset. " +
                "No message text, image bytes, media paths, URL tokens, or credentials returned.\n" +
                if (rows.isEmpty()) "No non-deleted media locations in this page." else rows.joinToString("\n")
        }
        CallToolResult(content = listOf(TextContent(text = report)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent(text = "Media metadata inspection failed (${e.javaClass.simpleName}). Specify a valid roomIds filter and verify Beeper read permission.")), isError = true)
    }
}
