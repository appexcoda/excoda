package app.excoda.features.registra

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface RegistraApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("search")
    suspend fun search(
        @Query("text") text: String? = null,
        @Query("artist") artist: String? = null,
        @Query("title") title: String? = null,
        @Query("file_type") fileType: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<SearchResponse>

    @Multipart
    @POST("files")
    suspend fun upload(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @GET("files/{id}")
    @Streaming
    suspend fun download(@Path("id") id: String): Response<ResponseBody>

    @DELETE("files/{id}")
    suspend fun delete(@Path("id") id: Long): Response<DeleteResponse>
}

data class HealthResponse(
    val status: String
)

data class SearchResponse(
    val results: List<FileResult>?,
    val total: Int,
    val returned: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val text: String? = null,
    val artist: String? = null,
    val title: String? = null,
    @SerializedName("file_type") val fileType: String? = null
)

data class FileResult(
    @SerializedName("ID") val id: Long,
    @SerializedName("FilePath") val filePath: String,
    @SerializedName("FileName") val fileName: String,
    @SerializedName("FileType") val fileType: String,
    @SerializedName("Artist") val artist: String?,
    @SerializedName("Title") val title: String?,
    @SerializedName("SubTitle") val subtitle: String?,
    @SerializedName("Album") val album: String?,
    @SerializedName("FileHash") val fileHash: String?,
    @SerializedName("FileSize") val fileSize: Long,
    @SerializedName("IndexedAt") val indexedAt: String,
    @SerializedName("ModifiedAt") val modifiedAt: String,
    @SerializedName("IsUploaded") val isUploaded: Boolean = false
)

data class UploadResponse(
    val message: String,
    val filename: String,
    val path: String
)

data class DeleteResponse(
    val message: String
)

data class ErrorResponse(
    val error: String
)

data class ConflictResponse(
    val error: String,
    @SerializedName("existing_file") val existingFile: ExistingFileInfo
)

data class ExistingFileInfo(
    val id: Long,
    val filename: String
)