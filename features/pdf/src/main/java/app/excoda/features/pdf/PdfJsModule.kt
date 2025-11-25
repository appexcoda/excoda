package app.excoda.features.pdf

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.facegestures.WithFaceGestures
import app.excoda.core.launcher.api.FileModule
import app.excoda.core.launcher.api.FileModuleDescriptor
import app.excoda.core.launcher.api.FileModuleRegistry
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.SettingsContributor
import app.excoda.core.settings.SettingsRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FilterInputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

object PdfJsModule : FileModule {
    override val descriptor: FileModuleDescriptor = object : FileModuleDescriptor {
        override val supportedExtensions: Set<String> = setOf("pdf")
        override val displayName: String = "PDF.js"
        override val supportsFaceGestures: Boolean = true
    }

    init {
        FileModuleRegistry.register(this)
        SettingsRegistry.register(PdfJsSettingsContributor)
    }

    @Composable
    override fun Content(
        fileUri: String,
        faceGestureHost: FaceGestureHost,
        faceGestureController: FaceGestureController,
        fabMenuHost: FabMenuHost,
        onShowSnackbar: (String, Boolean) -> Unit,
        onSwitchFile: (Uri) -> Unit,
        onRegisterSaveCallback: (suspend () -> Boolean) -> Unit,
        onAddToRecents: (Uri, String) -> Unit
    ) {
        val context = LocalContext.current
        var showSaveDialog by remember { mutableStateOf(false) }
        val saveConfirmationDeferred = remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

        val holder = remember(context) {
            PdfJsWebViewHolder(
                context = context,
                onShowSnackbar = onShowSnackbar,
                onRequestSaveConfirmation = { deferred ->
                    saveConfirmationDeferred.value = deferred
                    showSaveDialog = true
                }
            )
        }

        LaunchedEffect(fileUri) {
            LxLog.d("PdfJs", "Registering save callback for $fileUri")
            onRegisterSaveCallback {
                holder.checkAndSave()
            }
            holder.display(Uri.parse(fileUri))
        }

        DisposableEffect(holder) {
            onDispose { holder.destroy() }
        }

        WithFaceGestures(
            faceGestureHost = faceGestureHost,
            faceGestureController = faceGestureController,
            onGesture = { gestureType ->
                when (gestureType) {
                    app.excoda.core.facegestures.FaceGestureType.MouthDimpleRight -> {
                        LxLog.i("PageTurn", "Right mouth → Next page (PDF)")
                        holder.nextPage()
                    }
                    app.excoda.core.facegestures.FaceGestureType.MouthDimpleLeft -> {
                        LxLog.i("PageTurn", "Left mouth → Previous page (PDF)")
                        holder.previousPage()
                    }
                    app.excoda.core.facegestures.FaceGestureType.BrowRaiseRight -> {
                        LxLog.i("PageTurn", "Brow raise → Next page (PDF)")
                        holder.nextPage()
                    }
                    app.excoda.core.facegestures.FaceGestureType.Smile -> {
                        LxLog.i("PageTurn", "Smile → Previous page (PDF)")
                        holder.previousPage()
                    }
                    // Remove the 'else ->' line, let it be exhaustive without explicit else
                }
            }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { holder.webView }
            )
        }

        if (showSaveDialog) {
            PdfSaveConfirmationDialog(
                onSave = {
                    showSaveDialog = false
                    holder.performSave {
                        saveConfirmationDeferred.value?.complete(true)
                    }
                },
                onDiscard = {
                    showSaveDialog = false
                    saveConfirmationDeferred.value?.complete(true)
                },
                onDismiss = {
                    showSaveDialog = false
                    saveConfirmationDeferred.value?.complete(false)
                }
            )
        }
    }

    override fun settingsContributors(): List<SettingsContributor> =
        listOf(PdfJsSettingsContributor)
}

private class PdfJsWebViewHolder(
    private val context: Context,
    private val onShowSnackbar: (String, Boolean) -> Unit,
    private val onRequestSaveConfirmation: (CompletableDeferred<Boolean>) -> Unit
) {
    private var currentUri: Uri? = null
    private var isViewerLoaded = false
    private var pendingSaveData: String? = null

    private val assetLoader: WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler(
                "/pdfjs/",
                object : WebViewAssetLoader.PathHandler {
                    override fun handle(path: String): WebResourceResponse? {
                        val assetPath = "pdfjs/$path"
                        return try {
                            val stream = context.assets.open(assetPath)
                            val mime = guessMime(assetPath)
                            WebResourceResponse(mime, "utf-8", stream)
                        } catch (error: Exception) {
                            LxLog.w("PdfJsAsset", "Failed to serve $assetPath", error)
                            null
                        }
                    }
                }
            )
            .addPathHandler(
                "/excoda/",
                object : WebViewAssetLoader.PathHandler {
                    override fun handle(path: String): WebResourceResponse? {
                        if (path != "document.pdf") return null
                        val target = currentUri ?: return null
                        return servePdf(target)
                    }
                }
            )
            .build()

    val webView: WebView = WebView(context)

    init {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(DownloadInterface(), "AndroidDownload")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains(VIEWER_URL) == true) {
                    isViewerLoaded = true
                    LxLog.d("PdfJs", "Viewer page loaded and ready")
                }
            }
        }
    }

    suspend fun display(uri: Uri) = withContext(Dispatchers.Main) {
        if (currentUri == uri) {
            LxLog.d("PdfJs", "Requested URI already active; no reload needed")
            return@withContext
        }

        val previousUri = currentUri
        currentUri = uri

        val filename = getActualFilename(uri)
        LxLog.i("PdfJs", "Loading file: $filename")

        if (previousUri == null || !isViewerLoaded) {
            val viewerUrl = buildViewerUrl()
            LxLog.i("PdfJs", "Initial load: $viewerUrl for $uri")
            webView.loadUrl(viewerUrl)
        } else {
            switchDocument(uri)
        }
    }

    suspend fun checkAndSave(): Boolean {
        return withContext(Dispatchers.Main) {
            if (!isViewerLoaded) {
                LxLog.w("PdfJs", "Viewer not loaded")
                return@withContext true
            }

            val deferred = CompletableDeferred<Boolean>()

            val js = """
                (function() {
                    if (typeof PDFViewerApplication === 'undefined' || !PDFViewerApplication.pdfDocument) {
                        return 'ERROR';
                    }
                    
                    var hasChanges = false;
                    try {
                        var storage = PDFViewerApplication.pdfDocument.annotationStorage;
                        hasChanges = storage && storage.size > 0;
                    } catch (e) {
                        console.error('[ExcodaPdf] Error checking storage:', e);
                    }
                    
                    console.log('[ExcodaPdf] Has changes: ' + hasChanges);
                    
                    if (hasChanges) {
                        PDFViewerApplication.pdfDocument.saveDocument().then(function(data) {
                            var binary = '';
                            var bytes = new Uint8Array(data);
                            var len = bytes.byteLength;
                            for (var i = 0; i < len; i++) {
                                binary += String.fromCharCode(bytes[i]);
                            }
                            var base64 = 'data:application/pdf;base64,' + btoa(binary);
                            AndroidDownload.cacheForConfirmation(base64);
                        }).catch(function(error) {
                            console.error('[ExcodaPdf] Failed to extract PDF:', error);
                        });
                    }
                    
                    return hasChanges ? 'YES' : 'NO';
                })();
            """.trimIndent()

            webView.evaluateJavascript(js) { result ->
                val hasChanges = result?.trim('"') == "YES"
                LxLog.d("PdfJs", "Has changes: $hasChanges")

                if (hasChanges) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        onRequestSaveConfirmation(deferred)
                    }, 300)
                } else {
                    deferred.complete(true)
                }
            }

            deferred.await()
        }
    }

    fun performSave(onComplete: () -> Unit) {
        val data = pendingSaveData
        val uri = currentUri

        if (data == null || uri == null) {
            LxLog.e("PdfJs", "No pending save data available")
            onComplete()
            return
        }

        LxLog.i("PdfJs", "Saving to current file")

        try {
            val base64 = if (data.contains(",")) {
                data.substring(data.indexOf(",") + 1)
            } else {
                data
            }

            val bytes = Base64.decode(base64, Base64.DEFAULT)

            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(bytes)
                LxLog.i("PdfJsDownload", "Saved ${bytes.size} bytes to current file")
            }

            pendingSaveData = null
            onComplete()

        } catch (e: Exception) {
            LxLog.e("PdfJsDownload", "Failed to save", e)
            onComplete()
        }
    }

    fun nextPage() {
        if (!isViewerLoaded) return
        val js = """
            (function() {
                if (typeof PDFViewerApplication !== 'undefined' && PDFViewerApplication.pdfViewer) {
                    var viewer = PDFViewerApplication.pdfViewer;
                    if (viewer.currentPageNumber < viewer.pagesCount) {
                        viewer.currentPageNumber++;
                        return 'OK';
                    } else {
                        return 'LAST_PAGE';
                    }
                }
                return 'ERROR';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { result ->
            LxLog.d("PdfJsPagination", "Next page result: $result")
        }
    }

    fun previousPage() {
        if (!isViewerLoaded) return
        val js = """
            (function() {
                if (typeof PDFViewerApplication !== 'undefined' && PDFViewerApplication.pdfViewer) {
                    var viewer = PDFViewerApplication.pdfViewer;
                    if (viewer.currentPageNumber > 1) {
                        viewer.currentPageNumber--;
                        return 'OK';
                    } else {
                        return 'FIRST_PAGE';
                    }
                }
                return 'ERROR';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { result ->
            LxLog.d("PdfJsPagination", "Previous page result: $result")
        }
    }

    fun destroy() {
        isViewerLoaded = false
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.destroy()
    }

    private fun switchDocument(uri: Uri) {
        val cacheBust = System.currentTimeMillis()
        val documentUrl = "$DOCUMENT_URL?cacheBust=$cacheBust"
        val js = """
            (function() {
                if (window.ExcodaPdf && window.ExcodaPdf.openDocument) {
                    window.ExcodaPdf.openDocument('$documentUrl');
                }
            })();
        """.trimIndent()
        LxLog.i("PdfJs", "Switching to new document via API: $uri")
        webView.evaluateJavascript(js, null)
    }

    private fun buildViewerUrl(): String {
        val encodedFile = URLEncoder.encode(DOCUMENT_URL, "UTF-8")
        val cacheBust = System.currentTimeMillis()
        return buildString {
            append(VIEWER_URL)
            append("?file=")
            append(encodedFile)
            append("&disableStream=true&disableAutoFetch=true&disableRange=true")
            append("&cacheBust=")
            append(cacheBust)
        }
    }

    private inner class DownloadInterface {
        @android.webkit.JavascriptInterface
        fun saveFile(base64Data: String) {
            LxLog.i("PdfJsDownload", "Manual save: received blob data, length=${base64Data.length}")

            try {
                val base64 = if (base64Data.contains(",")) {
                    base64Data.substring(base64Data.indexOf(",") + 1)
                } else {
                    base64Data
                }

                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val uri = currentUri ?: return

                Handler(Looper.getMainLooper()).post {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        outputStream.write(bytes)
                        LxLog.i("PdfJsDownload", "Saved ${bytes.size} bytes")
                        onShowSnackbar("File Saved", false)
                    } ?: run {
                        onShowSnackbar("Failed to save file", true)
                    }
                }
            } catch (e: Exception) {
                LxLog.e("PdfJsDownload", "Failed to save", e)
                Handler(Looper.getMainLooper()).post {
                    onShowSnackbar("Save failed: ${e.message}", true)
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun cacheForConfirmation(base64Data: String) {
            LxLog.i("PdfJsDownload", "Caching data for confirmation, length=${base64Data.length}")
            pendingSaveData = base64Data
        }
    }

    private fun getActualFilename(uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else {
                        "document.pdf"
                    }
                } else {
                    "document.pdf"
                }
            } ?: "document.pdf"
        } catch (e: Exception) {
            LxLog.w("PdfJsDownload", "Failed to get filename from URI: $uri", e)
            "document.pdf"
        }
    }

    private fun servePdf(uri: Uri): WebResourceResponse? {
        return try {
            val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: return null
            val length = descriptor.length
            val stream = object : FilterInputStream(descriptor.createInputStream()) {
                override fun close() {
                    super.close()
                    descriptor.close()
                }
            }
            val headers = mutableMapOf(
                "Content-Type" to "application/pdf",
                "Accept-Ranges" to "none",
                "Cache-Control" to "no-store",
                "Pragma" to "no-cache",
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET"
            )
            if (length > 0) {
                headers["Content-Length"] = length.toString()
            }
            LxLog.d("PdfJsAsset", "Streaming PDF $uri length=$length")
            WebResourceResponse(
                "application/pdf",
                null,
                200,
                "OK",
                headers,
                stream
            )
        } catch (error: IOException) {
            LxLog.e("PdfJsAsset", "Failed to stream $uri", error)
            null
        }
    }

    private fun guessMime(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "js", "mjs" -> "application/javascript"
                "css" -> "text/css"
                "json" -> "application/json"
                "woff2" -> "font/woff2"
                "woff" -> "font/woff"
                "otf" -> "font/otf"
                else -> "application/octet-stream"
            }
    }

    private companion object {
        private const val VIEWER_URL =
            "https://appassets.androidplatform.net/pdfjs/web/viewer.html"
        private const val DOCUMENT_URL =
            "https://appassets.androidplatform.net/excoda/document.pdf"
    }
}