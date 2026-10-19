package com.beeper.mcp

import android.content.Context
import android.database.Cursor
import androidx.core.net.toUri
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

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
