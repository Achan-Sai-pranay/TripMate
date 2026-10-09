package com.example.tripmate.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.tripmate.model.DocumentFileType
import java.io.File
import java.text.DecimalFormat

data class StoredFileResult(
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val fileType: DocumentFileType
)

object TripDocumentFileManager {

    /**
     * Copies a picked Uri (content://) to the app's internal files storage:
     * filesDir/trip_documents/<tripId>/<docId>_<sanitizedName>
     * This avoids content URI permission expiration across reboots.
     */
    fun copyPickedFile(
        context: Context,
        sourceUri: Uri,
        tripId: String,
        docId: String
    ): StoredFileResult? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "document"
            var fileSize = 0L

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val sanitizedName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val mimeType = contentResolver.getType(sourceUri)?.lowercase() ?: ""
            val ext = displayName.substringAfterLast('.', "").lowercase()

            val fileType = when {
                mimeType == "application/pdf" || ext == "pdf" -> DocumentFileType.PDF
                mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> DocumentFileType.IMAGE
                else -> DocumentFileType.NONE
            }

            val tripDir = File(context.filesDir, "trip_documents/$tripId").apply { mkdirs() }
            val destFile = File(tripDir, "${docId}_$sanitizedName")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize <= 0L && destFile.exists()) {
                fileSize = destFile.length()
            }

            StoredFileResult(
                filePath = destFile.absolutePath,
                fileName = displayName,
                fileSizeBytes = fileSize,
                fileType = fileType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local document file if it exists.
     */
    fun deleteStoredFile(filePath: String?) {
        if (!filePath.isNullOrBlank()) {
            try {
                val f = File(filePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
    }

    /**
     * Launches external PDF viewer using FileProvider.
     */
    fun openPdfFile(context: Context, filePath: String): Result<Unit> {
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalStateException("File not found at $filePath")
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Human readable format for file size.
     */
    fun formatFileSize(bytes: Long?): String {
        if (bytes == null || bytes <= 0L) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "${DecimalFormat("#.#").format(mb)} MB"
            kb >= 1.0 -> "${DecimalFormat("#.#").format(kb)} KB"
            else -> "$bytes B"
        }
    }
}
