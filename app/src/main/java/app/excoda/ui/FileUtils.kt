package app.excoda.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color


fun getFileExtension(fileName: String): String {
    return fileName.substringAfterLast('.', "").lowercase()
}

fun getFileIconColor(fileName: String): Color {
    return when (getFileExtension(fileName)) {
        "gp3" -> Color(0xFF9C27B0)
        "gp4" -> Color(0xFF2BAF7A)
        "gp5" -> Color(0xFF5E35B1)
        "gpx" -> Color(0xFF388E3C)
        "gp" -> Color(0xFF00838F)
        "mscz" -> Color(0xFF1565C0)
        "musicxml" -> Color(0xFFFF8F00)
        "pdf" -> Color(0xFFD32F2F)
        else -> Color(0xFF1976D2)
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = "Unknown file"
    
    if (uri.toString().isBlank()) {
        return fileName
    }
    
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (_: Exception) {
    }
    
    if (fileName == "Unknown file") {
        fileName = uri.lastPathSegment ?: "Unknown file"
    }
    
    return fileName
}

fun isValidFileExtension(fileName: String): Boolean {
    val allowedExtensions = listOf("pdf", "gp", "gp3", "gp4", "gp5", "gpx", "musicxml", "mscz")
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in allowedExtensions
}