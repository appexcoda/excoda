package app.excoda.features.alphatab

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.*
import app.excoda.core.launcher.api.*
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsRepository
import app.excoda.core.settings.SettingsRegistry
import app.excoda.features.registra.RegistraApiFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

data class TrackInfo(val index: Int, val name: String)

object AlphaTabModule : FileModule {
    override val descriptor = object : FileModuleDescriptor {
        override val supportedExtensions = setOf("gp3", "gp4", "gp5", "gpx", "gp", "musicxml", "xml", "mscz")
        override val displayName = "AlphaTab"
        override val supportsFaceGestures = true
    }

    lateinit var globalSettingsRepository: GlobalSettingsRepository
    lateinit var registraApiFactory: RegistraApiFactory

    fun initialize(globalSettings: GlobalSettingsRepository, apiFactory: RegistraApiFactory) {
        globalSettingsRepository = globalSettings
        registraApiFactory = apiFactory
    }

    init {
        FileModuleRegistry.register(this)
        SettingsRegistry.register(AlphaTabSettingsContributor)
        AlphaTabMigrations.register()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    @Composable
    override fun Content(
        fileUri: String,
        faceGestureHost: FaceGestureHost,
        faceGestureController: FaceGestureController,
        fabMenuHost: FabMenuHost,
        onShowSnackbar: (String, Boolean) -> Unit,
        onSwitchFile: (Uri) -> Unit,
        onRegisterSaveCallback: (suspend () -> Boolean) -> Unit
    ) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val settingsViewModel: AlphaTabSettingsViewModel = hiltViewModel()

        val fileSettings by settingsViewModel.observeFileSettings(fileUri)
            .collectAsStateWithLifecycle(AlphaTabSettings.DEFAULT, lifecycle)

        val autoEnableGestures by globalSettingsRepository.autoEnableGestures
            .collectAsStateWithLifecycle(initialValue = false, lifecycle = lifecycle)
        val gestureConsentGiven by globalSettingsRepository.gestureConsentGiven
            .collectAsStateWithLifecycle(initialValue = false, lifecycle = lifecycle)

        val holder = remember(context) { AlphaTabWebViewHolder(context) }
        var isLoading by remember { mutableStateOf(false) }

        LaunchedEffect(holder) {
            holder.onTracksAvailable = { settingsViewModel.setAvailableTracks(it) }
            holder.onError = {
                onShowSnackbar("Unsupported file format", true)
                onSwitchFile(Uri.EMPTY)
            }
            holder.onLoadingChanged = { isLoading = it }
        }

        val isCenterZoneHidden by fabMenuHost.isCenterZoneHidden.collectAsStateWithLifecycle()
        val isFabExpanded by fabMenuHost.isExpanded.collectAsStateWithLifecycle()

        DisposableEffect(holder) {
            onDispose {
                holder.destroy()
                fabMenuHost.setCenterZoneHidden(false)
            }
        }

        LaunchedEffect(fileUri) {
            try {
                LxLog.i("AlphaTab", "Loading $fileUri")
                val uri = fileUri.toUri()
                
                holder.setCurrentFileUri(fileUri)
                
                val extension = withContext(Dispatchers.IO) {
                    DocumentFile.fromSingleUri(context, uri)
                        ?.name
                        ?.substringAfterLast('.', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                        ?.lowercase(Locale.ROOT)
                }
                
                if (extension == "mscz") {
                    val registraHost = globalSettingsRepository.registraHost.first()
                    val registraApiKey = globalSettingsRepository.registraApiKey.first()
                    
                    if (registraHost.isEmpty() || registraApiKey.isEmpty()) {
                        onShowSnackbar("Registra settings not configured", true)
                        onSwitchFile(Uri.EMPTY)
                        return@LaunchedEffect
                    }
                    
                    holder.loadMsczFile(uri, context, registraHost, registraApiKey, registraApiFactory) { errorMessage ->
                        onShowSnackbar(errorMessage, true)
                        onSwitchFile(Uri.EMPTY)
                    }
                } else {
                    holder.loadFile(uri)
                }
            } catch (error: Exception) {
                LxLog.e("AlphaTab", "Failed to load $fileUri", error)
            }
        }

        LaunchedEffect(fileUri, autoEnableGestures, gestureConsentGiven) {
            if (autoEnableGestures && gestureConsentGiven) {
                LxLog.d("AlphaTab", "Auto-enabling gestures for $fileUri")
                faceGestureController.enable()
            }
        }

        LaunchedEffect(fileUri, fileSettings.selectedTrackIndex) {
            LxLog.d("AlphaTab", "===== Settings flow triggered: trackIndex=${fileSettings.selectedTrackIndex}, settings=$fileSettings =====")

            settingsViewModel.setCurrentTrack(fileSettings.selectedTrackIndex)

            settingsViewModel.observeTrackSettings(fileUri, fileSettings.selectedTrackIndex)
                .collect { trackSettings ->
                    LxLog.d("AlphaTab", "===== Track settings received: $trackSettings =====")
                    holder.applySettings(trackSettings, fileSettings.selectedTrackIndex, fileUri)
                }
        }

        WithFaceGestures(
            faceGestureHost = faceGestureHost,
            faceGestureController = faceGestureController,
            onGesture = { gestureType ->
                when (gestureType) {
                    FaceGestureType.MouthStretchRight -> {
                        LxLog.i("PageTurn", "Right mouth stretch → Next page")
                        holder.nextPage()
                    }
                    FaceGestureType.MouthStretchLeft -> {
                        LxLog.i("PageTurn", "Left mouth stretch → Previous page")
                        holder.previousPage()
                    }
                    FaceGestureType.BrowRaiseRight -> {
                        LxLog.i("PageTurn", "Brow raise → Next page")
                        holder.nextPage()
                    }
                    FaceGestureType.Smile -> {
                        LxLog.i("PageTurn", "Smile → Previous page")
                        holder.previousPage()
                    }
                }
            },
            isCenterZoneHidden = isCenterZoneHidden
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val interceptor = PaginationInterceptor(
                        onPreviousPage = { holder.previousPage() },
                        onNextPage = { holder.nextPage() },
                        onCenterZoneTap = {
                            val newState = !isCenterZoneHidden
                            fabMenuHost.setCenterZoneHidden(newState)
                            LxLog.d("AlphaTab", "Center zone toggle: hidden=$newState")
                        },
                        isFabExpanded = { isFabExpanded }
                    )

                    holder.webView.apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setOnTouchListener(interceptor)
                    }
                }
            )
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    override fun settingsContributors() = listOf(AlphaTabSettingsContributor)
}

private class AlphaTabWebViewHolder(private val context: Context) {
    val webView = WebView(context)

    private var currentFileUri: String? = null
    private var scoreData: ByteArray? = null
    private var lastAppliedSettings: Pair<AlphaTabTrackSettings, Int>? = null
    private var webViewReady = false

    private var pendingFileLoad: ByteArray? = null
    private var pendingSettings: Triple<AlphaTabTrackSettings, Int, String>? = null

    var onTracksAvailable: ((List<TrackInfo>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onLoadingChanged: ((Boolean) -> Unit)? = null

    init {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = true
        }

        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.webViewClient = createWebViewClient()
        webView.webChromeClient = createWebChromeClient()

        LxLog.d("AlphaTabWebView", "Loading viewer.html")
        webView.loadUrl("https://appassets.androidplatform.net/alphatab/viewer.html")
    }

    fun setCurrentFileUri(fileUri: String) {
        if (currentFileUri != fileUri) {
            currentFileUri = fileUri
            scoreData = null
            lastAppliedSettings = null
        }
    }

    suspend fun loadFile(uri: Uri) {
        val newFileUri = uri.toString()
        val isFileSwitch = currentFileUri != null && currentFileUri != newFileUri
        scoreData = null
        lastAppliedSettings = null

        if (isFileSwitch) {
            LxLog.d("AlphaTab", "File switch detected, resetting viewer")
            withContext(Dispatchers.Main) {
                callJs("window.AlphaTabBridge.resetViewer()") { result ->
                    LxLog.d("AlphaTab", "Reset complete: $result")
                }
            }
        }

        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)
        }

        if (bytes == null) {
            LxLog.w("AlphaTab", "No data for $uri")
            return
        }

        scoreData = bytes
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        LxLog.d("AlphaTab", "Score bytes loaded (${bytes.size} bytes)")

        withContext(Dispatchers.Main) {
            if (webViewReady) {
                LxLog.d("AlphaTab", "Injecting score (${base64.length} chars)")
                storeScoreData(base64)

                pendingSettings?.let { (settings, trackIndex, fileUri) ->
                    LxLog.d("AlphaTab", "Applying queued settings after injection")
                    applySettings(settings, trackIndex, fileUri)
                    pendingSettings = null
                }
            } else {
                LxLog.d("AlphaTab", "Viewer not ready; caching payload")
                pendingFileLoad = bytes
            }
        }
    }

    suspend fun loadMsczFile(
        uri: Uri,
        context: Context,
        registraHost: String,
        registraApiKey: String,
        apiFactory: RegistraApiFactory,
        onError: (String) -> Unit
    ) {
        val isFileSwitch = currentFileUri != null && currentFileUri != uri.toString()

        scoreData = null
        lastAppliedSettings = null

        if (isFileSwitch) {
            LxLog.d("AlphaTab", "File switch detected, resetting viewer")
            withContext(Dispatchers.Main) {
                callJs("window.AlphaTabBridge.resetViewer()") { result ->
                    LxLog.d("AlphaTab", "Reset complete: $result")
                }
            }
        }

        onLoadingChanged?.invoke(true)

        try {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)
            }

            if (bytes == null) {
                LxLog.w("AlphaTab", "No data for $uri")
                onError("Failed to read .mscz file")
                onLoadingChanged?.invoke(false)
                return
            }

            val tempFile = withContext(Dispatchers.IO) {
                File.createTempFile("mscz_", ".mscz", context.cacheDir).apply {
                    writeBytes(bytes)
                }
            }

            try {
                val api = apiFactory.create(registraHost, registraApiKey) { false }
                
                val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                val response = withContext(Dispatchers.IO) {
                    api.convertMscz(multipartBody)
                }

                if (response.isSuccessful) {
                    val convertedBytes = response.body()?.bytes()
                    if (convertedBytes != null) {
                        scoreData = convertedBytes
                        val base64 = Base64.encodeToString(convertedBytes, Base64.NO_WRAP)
                        LxLog.d("AlphaTab", "Converted score loaded (${convertedBytes.size} bytes)")

                        withContext(Dispatchers.Main) {
                            if (webViewReady) {
                                LxLog.d("AlphaTab", "Injecting converted score (${base64.length} chars)")
                                storeScoreData(base64)

                                pendingSettings?.let { (settings, trackIndex, fileUri) ->
                                    LxLog.d("AlphaTab", "Applying queued settings after injection")
                                    applySettings(settings, trackIndex, fileUri)
                                    pendingSettings = null
                                }
                            } else {
                                LxLog.d("AlphaTab", "Viewer not ready; caching payload")
                                pendingFileLoad = convertedBytes
                            }
                        }
                    } else {
                        LxLog.e("AlphaTab", "Empty response from Registra conversion")
                        onError("Conversion failed: empty response")
                        onLoadingChanged?.invoke(false)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        errorBody?.let {
                            JSONObject(it).getString("error")
                        } ?: "Unknown error"
                    } catch (e: Exception) {
                        LxLog.e("AlphaTab", "Error parsing error response", e)
                        errorBody ?: "Unknown error"
                    }
                    LxLog.e("AlphaTab", "Registra conversion failed: ${response.code()} - $errorMsg")
                    onError("Conversion failed: $errorMsg")
                    onLoadingChanged?.invoke(false)
                }
            } finally {
                withContext(Dispatchers.IO) {
                    tempFile.delete()
                }
            }
        } catch (e: CancellationException) {
            LxLog.d("AlphaTab", "MSCZ conversion cancelled (user navigated away)", e)
            onLoadingChanged?.invoke(false)
        } catch (e: UnknownHostException) {
            LxLog.e("AlphaTab", "Failed to connect to Registra", e)
            onError("Cannot connect to Registra server")
            onLoadingChanged?.invoke(false)
        } catch (e: SocketTimeoutException) {
            LxLog.e("AlphaTab", "Registra connection timeout", e)
            onError("Registra server timeout")
            onLoadingChanged?.invoke(false)
        } catch (e: Exception) {
            LxLog.e("AlphaTab", "Failed to convert .mscz file", e)
            onError("Failed to convert file")
            onLoadingChanged?.invoke(false)
        }
    }

    fun applySettings(settings: AlphaTabTrackSettings, trackIndex: Int, fileUri: String) {
        if (fileUri != currentFileUri) {
            LxLog.d("AlphaTab", "Skipping settings - file URI mismatch")
            return
        }

        if (scoreData == null || !webViewReady) {
            LxLog.d("AlphaTab", "Score/webView not ready, queueing settings")
            pendingSettings = Triple(settings, trackIndex, fileUri)
            return
        }

        if (lastAppliedSettings == settings to trackIndex) {
            LxLog.d("AlphaTab", "Settings unchanged, skipping re-render")
            return
        }

        val barsPerRow = if (settings.isAutomaticBarsPerRow) -1 else settings.barsPerRow
        LxLog.d("AlphaTab", "Applying settings - barsPerRow: $barsPerRow, track: $trackIndex")

        val settingsJson = buildSettingsJson(settings, trackIndex)
        LxLog.d("AlphaTab", "Applying settings via AlphaTabBridge")

        callJs("window.AlphaTabBridge.applyAllSettings($settingsJson)") { result ->
            val cleanResult = result?.trim('"')
            LxLog.d("AlphaTab", "Settings applied - result: $cleanResult")
            if (cleanResult?.startsWith("ERROR") == true) {
                LxLog.e("AlphaTab", "Failed to apply settings: $cleanResult")
            }
        }

        lastAppliedSettings = settings to trackIndex
    }

    fun nextPage() {
        if (!webViewReady) return
        callJs("window.AlphaTabBridge.nextPage()") { result ->
            LxLog.d("AlphaTabPagination", "Next page result: $result")
        }
    }

    fun previousPage() {
        if (!webViewReady) return
        callJs("window.AlphaTabBridge.previousPage()") { result ->
            LxLog.d("AlphaTabPagination", "Previous page result: $result")
        }
    }

    fun destroy() {
        LxLog.d("AlphaTabWebView", "Destroying WebView")
        webView.removeJavascriptInterface("Android")
        webView.destroy()
    }


    private fun storeScoreData(base64: String) {
        val js = "window.AlphaTabBridge.loadBase64(${JSONObject.quote(base64)})"
        webView.evaluateJavascript(js) { result ->
            LxLog.d("AlphaTab", "Injection callback: $result")
        }
    }

    private fun callJs(script: String, callback: ((String?) -> Unit)? = null) {
        if (!webViewReady) return
        webView.evaluateJavascript(script, callback)
    }

    private fun buildSettingsJson(settings: AlphaTabTrackSettings, trackIndex: Int): String {
        return JSONObject().apply {
            put("selectedTrackIndex", trackIndex)
            put("staveProfile", settings.staveProfile)
            put("scale", settings.scale)
            put("stretch", settings.stretch)
            put("layoutMode", settings.layoutMode)
            put("barsPerRow", if (settings.isAutomaticBarsPerRow) -1 else settings.barsPerRow)
            put("tabRhythmMode", settings.tabRhythmMode)
            put("padding", JSONArray(settings.padding))
            put("systemsSpacing", settings.systemsSpacing)
            put("isAutomaticSystemsSpacing", settings.isAutomaticSystemsSpacing)
            put("showTempo", settings.showTempo)
            put("showTuning", settings.showTuning)
            put("showChordDiagrams", settings.showChordDiagrams)
            put("showChordNames", settings.showChordNames)
            put("showDynamics", settings.showDynamics)
            put("showBeatBarre", settings.showBeatBarre)
            put("showText", settings.showText)
            put("showPickStroke", settings.showPickStroke)
            put("showLyrics", settings.showLyrics)
            put("showTrackNames", settings.showTrackNames)
        }.toString()
    }

    private fun createWebViewClient() = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            LxLog.d("AlphaTabWebView", "Intercept request ${request.url}")
            return createAssetLoader().shouldInterceptRequest(request.url)
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            LxLog.i("AlphaTabWebView", "Page started: $url")
            webViewReady = false
        }

        override fun onPageFinished(view: WebView, url: String?) {
            LxLog.i("AlphaTabWebView", "Page finished: $url")
            webViewReady = true

            pendingFileLoad?.let { bytes ->
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                LxLog.d("AlphaTab", "Injecting score (${base64.length} chars)")
                storeScoreData(base64)
                pendingFileLoad = null

                pendingSettings?.let { (settings, trackIndex, fileUri) ->
                    LxLog.d("AlphaTab", "Applying queued settings after page ready")
                    applySettings(settings, trackIndex, fileUri)
                    pendingSettings = null
                }
            }
        }
    }

    private fun createWebChromeClient() = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            if (BuildConfig.DEBUG) {
                val message = consoleMessage.message()
                val location = "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    LxLog.e("AlphaTabWebView", "$message @$location")
                } else {
                    LxLog.d("AlphaTabWebView", "$message @$location")
                }
            }
            return true
        }
    }

    private fun createAssetLoader() = WebViewAssetLoader.Builder()
        .addPathHandler("/alphatab/") { path ->
            val assetPath = "alphatab/$path"
            try {
                val stream = context.assets.open(assetPath)
                val mime = guessMime(assetPath)
                LxLog.d("AlphaTabAsset", "Serving $assetPath as $mime")
                WebResourceResponse(mime, "utf-8", stream)
            } catch (_: FileNotFoundException) {
                LxLog.w("AlphaTabAsset", "Missing asset $assetPath")
                null
            } catch (error: Exception) {
                LxLog.e("AlphaTabAsset", "Error opening $assetPath", error)
                null
            }
        }
        .build()

    private fun guessMime(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "js" -> "application/javascript"
            "json" -> "application/json"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "otf" -> "font/otf"
            else -> "application/octet-stream"
        }
    }

    inner class AndroidBridge {
        @Suppress("unused")
        @JavascriptInterface
        fun onTracksAvailable(json: String) {
            try {
                val tracks = JSONArray(json).let { array ->
                    (0 until array.length()).map { i ->
                        array.getJSONObject(i).let {
                            TrackInfo(it.getInt("index"), it.getString("name"))
                        }
                    }
                }
                LxLog.d("AlphaTab", "Tracks available: ${tracks.size}")
                Handler(Looper.getMainLooper()).post { onTracksAvailable?.invoke(tracks) }
            } catch (e: Exception) {
                LxLog.e("AlphaTab", "Failed to parse tracks", e)
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onLoadingChanged(loading: Boolean) {
            LxLog.d("AlphaTab", "Loading state: $loading")
            Handler(Looper.getMainLooper()).post { onLoadingChanged?.invoke(loading) }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onError(message: String) {
            LxLog.e("AlphaTab", "Error from viewer: $message")
            Handler(Looper.getMainLooper()).post { onError?.invoke(message) }
        }
    }
}