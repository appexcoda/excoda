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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.*
import app.excoda.core.launcher.api.*
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.SettingsContributor
import app.excoda.core.settings.SettingsRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Locale

data class TrackInfo(val index: Int, val name: String)

object AlphaTabModule : FileModule {
    override val descriptor = object : FileModuleDescriptor {
        override val supportedExtensions = setOf("gp3", "gp4", "gp5", "gpx", "gp", "musicxml", "xml")
        override val displayName = "AlphaTab"
        override val supportsFaceGestures = true
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
        onRegisterSaveCallback: (suspend () -> Boolean) -> Unit,
        onAddToRecents: (Uri, String) -> Unit
    ) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val settingsViewModel: AlphaTabSettingsViewModel = hiltViewModel()

        val fileSettings by settingsViewModel.observeFileSettings(fileUri)
            .collectAsStateWithLifecycle(AlphaTabSettings.DEFAULT, lifecycle)

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

        DisposableEffect(holder) {
            onDispose {
                holder.destroy()
                fabMenuHost.setCenterZoneHidden(false)
            }
        }

        LaunchedEffect(fileUri) {
            try {
                LxLog.i("AlphaTab", "Loading $fileUri")
                holder.loadFile(Uri.parse(fileUri))
            } catch (error: Exception) {
                LxLog.e("AlphaTab", "Failed to load $fileUri", error)
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
                    FaceGestureType.MouthDimpleRight -> {
                        LxLog.i("PageTurn", "Right mouth → Next page")
                        holder.nextPage()
                    }
                    FaceGestureType.MouthDimpleLeft -> {
                        LxLog.i("PageTurn", "Left mouth → Previous page")
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
                    holder.webView.apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setOnTouchListener { view, event ->
                            val y = event.y
                            val topBound = view.height * 0.25f
                            val bottomBound = view.height * 0.75f

                            if (y in topBound..bottomBound) {
                                PaginationInterceptor(
                                    onPreviousPage = { holder.previousPage() },
                                    onNextPage = { holder.nextPage() },
                                    onCenterZoneTap = {
                                        val newState = !isCenterZoneHidden
                                        fabMenuHost.setCenterZoneHidden(newState)
                                        LxLog.d("AlphaTab", "Center zone toggle: hidden=$newState")
                                    }
                                ).onTouch(view, event)
                            } else false
                        }
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

    // State
    private var currentFileUri: String? = null
    private var scoreData: ByteArray? = null
    private var lastAppliedSettings: Pair<AlphaTabTrackSettings, Int>? = null
    private var webViewReady = false

    // Pending operations (for when WebView loads)
    private var pendingFileLoad: ByteArray? = null
    private var pendingSettings: Triple<AlphaTabTrackSettings, Int, String>? = null

    // Callbacks
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

    suspend fun loadFile(uri: Uri) {
        val newFileUri = uri.toString()
        val isFileSwitch = currentFileUri != null && currentFileUri != newFileUri

        // Update state immediately
        currentFileUri = newFileUri
        scoreData = null
        lastAppliedSettings = null

        // Reset viewer on file switch
        if (isFileSwitch) {
            LxLog.d("AlphaTab", "File switch detected, resetting viewer")
            withContext(Dispatchers.Main) {
                callJs("window.AlphaTabBridge.resetViewer()") { result ->
                    LxLog.d("AlphaTab", "Reset complete: $result")
                }
            }
        }

        // Load file bytes
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

                // Apply pending settings if available
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

    fun applySettings(settings: AlphaTabTrackSettings, trackIndex: Int, fileUri: String) {
        // Guard: wrong file
        if (fileUri != currentFileUri) {
            LxLog.d("AlphaTab", "Skipping settings - file URI mismatch")
            return
        }

        // Guard: no score loaded yet OR webView not ready
        if (scoreData == null || !webViewReady) {
            LxLog.d("AlphaTab", "Score/webView not ready, queueing settings")
            pendingSettings = Triple(settings, trackIndex, fileUri)
            return
        }

        // Guard: settings unchanged
        if (lastAppliedSettings == settings to trackIndex) {
            LxLog.d("AlphaTab", "Settings unchanged, skipping re-render")
            return
        }

        // Apply settings
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

    // Private helpers

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

            // Process pending file load
            pendingFileLoad?.let { bytes ->
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                LxLog.d("AlphaTab", "Injecting score (${base64.length} chars)")
                storeScoreData(base64)
                pendingFileLoad = null
            }

            // Process pending settings
            pendingSettings?.let { (settings, trackIndex, fileUri) ->
                LxLog.d("AlphaTab", "Applying queued settings after page ready")
                applySettings(settings, trackIndex, fileUri)
                pendingSettings = null
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

        @JavascriptInterface
        fun onLoadingChanged(loading: Boolean) {
            LxLog.d("AlphaTab", "Loading state: $loading")
            Handler(Looper.getMainLooper()).post { onLoadingChanged?.invoke(loading) }
        }

        @JavascriptInterface
        fun onError(message: String) {
            LxLog.e("AlphaTab", "Error from viewer: $message")
            Handler(Looper.getMainLooper()).post { onError?.invoke(message) }
        }
    }
}