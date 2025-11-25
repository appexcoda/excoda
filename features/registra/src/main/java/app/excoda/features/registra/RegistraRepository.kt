package app.excoda.features.registra

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import app.excoda.core.logging.LxLog
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class RegistraResult<out T> {
    data class Success<T>(val data: T) : RegistraResult<T>()
    data class Error(val message: String) : RegistraResult<Nothing>()
    data class Conflict(val existingFileId: Long, val existingFileName: String) : RegistraResult<Nothing>()
}

@Singleton
class RegistraRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiFactory: RegistraApiFactory
) {
    suspend fun search(
        host: String?,
        apiKey: String?,
        text: String?,
        artist: String?,
        title: String?,
        fileType: String?,
        page: Int = 1
    ): RegistraResult<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            if (host.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra host not configured")
            }
            if (apiKey.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra API key not configured")
            }

            val api = apiFactory.create(host, apiKey)
            val response = api.search(
                text = text?.takeIf { it.isNotBlank() },
                artist = artist?.takeIf { it.isNotBlank() },
                title = title?.takeIf { it.isNotBlank() },
                fileType = fileType?.takeIf { it.isNotBlank() },
                page = page
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    RegistraResult.Success(it)
                } ?: RegistraResult.Error("Empty response")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).error
                } catch (e: Exception) {
                    response.message()
                }
                RegistraResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            LxLog.e("Registra", "Search failed", e)
            RegistraResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun upload(
        host: String?,
        apiKey: String?,
        file: File
    ): RegistraResult<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            if (host.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra host not configured")
            }
            if (apiKey.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra API key not configured")
            }

            val api = apiFactory.create(host, apiKey)
            val mimeType = when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "gp", "gpx" -> "application/octet-stream"
                else -> "application/octet-stream"
            }
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.upload(body)

            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        RegistraResult.Success(it)
                    } ?: RegistraResult.Error("Empty response")
                }

                response.code() == 409 -> {
                    val errorBody = response.errorBody()?.string()
                    val conflictResponse = try {
                        Gson().fromJson(errorBody, ConflictResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    if (conflictResponse?.existingFile != null) {
                        RegistraResult.Conflict(
                            existingFileId = conflictResponse.existingFile.id,
                            existingFileName = conflictResponse.existingFile.filename
                        )
                    } else {
                        val errorMsg = try {
                            Gson().fromJson(errorBody, ErrorResponse::class.java).error
                        } catch (e: Exception) {
                            "File already exists"
                        }
                        RegistraResult.Error(errorMsg)
                    }
                }

                else -> {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        response.message()
                    }
                    RegistraResult.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            LxLog.e("Registra", "Upload failed", e)
            RegistraResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun delete(
        host: String?,
        apiKey: String?,
        fileId: Long
    ): RegistraResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (host.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra host not configured")
            }
            if (apiKey.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra API key not configured")
            }

            val api = apiFactory.create(host, apiKey)
            val response = api.delete(fileId)

            if (response.isSuccessful) {
                RegistraResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).error
                } catch (e: Exception) {
                    response.message()
                }
                RegistraResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            LxLog.e("Registra", "Delete failed", e)
            RegistraResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun download(
        host: String?,
        apiKey: String?,
        fileId: String,
        fileName: String
    ): RegistraResult<String> = withContext(Dispatchers.IO) {
        try {
            if (host.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra host not configured")
            }
            if (apiKey.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Registra API key not configured")
            }

            val api = apiFactory.create(host, apiKey)
            val response = api.download(fileId)

            if (!response.isSuccessful) {
                return@withContext RegistraResult.Error(response.message())
            }

            val body = response.body() ?: return@withContext RegistraResult.Error("Empty response")

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName))
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext RegistraResult.Error("Failed to create file")

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                RegistraResult.Success(fileName)
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        } catch (e: Exception) {
            LxLog.e("Registra", "Download failed", e)
            RegistraResult.Error(e.message ?: "Download failed")
        }
    }

    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "gp", "gpx" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    suspend fun checkHealth(
        host: String?,
        apiKey: String?
    ): RegistraResult<String> = withContext(Dispatchers.IO) {
        try {
            if (host.isNullOrBlank()) {
                return@withContext RegistraResult.Error("Host is empty")
            }
            if (apiKey.isNullOrBlank()) {
                return@withContext RegistraResult.Error("API key is empty")
            }

            val api = apiFactory.create(host, apiKey)
            val response = withTimeout(10000) {
                api.health()
            }

            if (response.isSuccessful && response.body()?.status == "ok") {
                RegistraResult.Success("ok")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java).error
                } catch (e: Exception) {
                    response.message()
                }
                RegistraResult.Error(errorMsg)
            }
        } catch (e: java.net.UnknownHostException) {
            RegistraResult.Error("Cannot reach host")
        } catch (e: java.net.ConnectException) {
            RegistraResult.Error("Connection refused")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            RegistraResult.Error("Connection timeout")
        } catch (e: Exception) {
            LxLog.e("Registra", "Health check failed", e)
            RegistraResult.Error(e.message ?: "Connection failed")
        }
    }
}