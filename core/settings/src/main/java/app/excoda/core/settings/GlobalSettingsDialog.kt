package app.excoda.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

enum class SettingsTab {
    GESTURES, REGISTRA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsDialog(
    repository: GlobalSettingsRepository,
    connectionChecker: ConnectionChecker?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(SettingsTab.GESTURES) }

    // Gesture settings
    val gestureMode by repository.gestureMode.collectAsState(initial = GestureMode.MOUTH_MOVEMENTS)
    var localGestureMode by remember(gestureMode) { mutableStateOf(gestureMode) }
    var gestureExpanded by remember { mutableStateOf(false) }

    // Registra settings
    val registraHost by repository.registraHost.collectAsState(initial = "")
    val registraApiKey by repository.registraApiKey.collectAsState(initial = "")
    var localRegistraHost by remember(registraHost) { mutableStateOf(registraHost) }
    var localRegistraApiKey by remember(registraApiKey) { mutableStateOf(registraApiKey) }
    var isChecking by remember { mutableStateOf(false) }
    var checkPassed by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showCertDialog by remember { mutableStateOf(false) }
    var pendingCertInfo by remember { mutableStateOf<CertificateInfo?>(null) }
    var pendingCertContinuation by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val canCloseDialog = remember(selectedTab, localRegistraHost, localRegistraApiKey, checkPassed) {
        if (selectedTab == SettingsTab.GESTURES) {
            true
        } else {
            (localRegistraHost.isBlank() && localRegistraApiKey.isBlank()) ||
                    (localRegistraHost.isNotBlank() && localRegistraApiKey.isNotBlank() && checkPassed)
        }
    }

    val handleDismiss: () -> Unit = {
        if (canCloseDialog) {
            scope.launch {
                if (localRegistraHost.isBlank() && localRegistraApiKey.isBlank()) {
                    repository.setRegistraHost("")
                    repository.setRegistraApiKey("")
                }
            }
            onDismiss()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Check connection first")
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = handleDismiss) {
                                Icon(Icons.Rounded.Clear, "Close")
                            }
                        }
                        PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                            Tab(
                                selected = selectedTab == SettingsTab.GESTURES,
                                onClick = { selectedTab = SettingsTab.GESTURES },
                                text = { Text("Gestures") }
                            )
                            Tab(
                                selected = selectedTab == SettingsTab.REGISTRA,
                                onClick = { selectedTab = SettingsTab.REGISTRA },
                                text = { Text("Registra") }
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        SettingsTab.GESTURES -> GesturesTab(
                            localGestureMode = localGestureMode,
                            onGestureModeChange = {
                                localGestureMode = it
                                scope.launch { repository.setGestureMode(it) }
                            },
                            expanded = gestureExpanded,
                            onExpandedChange = { gestureExpanded = it }
                        )
                        SettingsTab.REGISTRA -> RegistraTab(
                            localHost = localRegistraHost,
                            onHostChange = {
                                localRegistraHost = it
                                checkPassed = false
                            },
                            localApiKey = localRegistraApiKey,
                            onApiKeyChange = {
                                localRegistraApiKey = it
                                checkPassed = false
                            },
                            isChecking = isChecking,
                            onCheck = {
                                if (connectionChecker != null) {
                                    scope.launch {
                                        isChecking = true
                                        val result = connectionChecker.checkConnection(
                                            localRegistraHost,
                                            localRegistraApiKey,
                                            onCertificateNeedsAcceptance = { certInfo ->
                                                pendingCertInfo = certInfo
                                                showCertDialog = true
                                                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                                                    pendingCertContinuation = { accepted ->
                                                        continuation.resumeWith(Result.success(accepted))
                                                        pendingCertContinuation = null
                                                    }
                                                }
                                            }
                                        )
                                        isChecking = false
                                        when (result) {
                                            is ConnectionResult.Success -> {
                                                checkPassed = true
                                                repository.setRegistraHost(localRegistraHost)
                                                repository.setRegistraApiKey(localRegistraApiKey)
                                                snackbarHostState.showSnackbar("Connection ok, saved")
                                            }

                                            is ConnectionResult.Error -> {
                                                checkPassed = false
                                                snackbarHostState.showSnackbar("Failed: ${result.message}")
                                            }
                                        }
                                    }
                                }
                            },
                            onScanQr = {
                                showQrScanner = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onScanned = { url, apiKey ->
                localRegistraHost = url
                localRegistraApiKey = apiKey
                checkPassed = false
            }
        )
    }

    if (showCertDialog && pendingCertInfo != null) {
        CertificateAcceptanceDialog(
            certInfo = pendingCertInfo!!,
            onAccept = {
                scope.launch {
                    repository.storeCertificate(
                        pendingCertInfo!!.host,
                        pendingCertInfo!!.fingerprint
                    )
                }
                showCertDialog = false
                pendingCertContinuation?.invoke(true)
            },
            onReject = {
                showCertDialog = false
                pendingCertContinuation?.invoke(false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GesturesTab(
    localGestureMode: GestureMode,
    onGestureModeChange: (GestureMode) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Active gestures",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = localGestureMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    GestureMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                onGestureModeChange(mode)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
            Text(
                text = localGestureMode.getDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun RegistraTab(
    localHost: String,
    onHostChange: (String) -> Unit,
    localApiKey: String,
    onApiKeyChange: (String) -> Unit,
    isChecking: Boolean,
    onCheck: () -> Unit,
    onScanQr: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = localHost,
            onValueChange = onHostChange,
            label = { Text("Registra host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://192.168.1.100:8080") },
            trailingIcon = {
                if (localHost.isNotBlank()) {
                    IconButton(onClick = { onHostChange("") }) {
                        Icon(Icons.Rounded.Clear, "Clear")
                    }
                }
            }
        )

        OutlinedTextField(
            value = localApiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Registra API key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (localApiKey.isNotBlank()) {
                    IconButton(onClick = { onApiKeyChange("") }) {
                        Icon(Icons.Rounded.Clear, "Clear")
                    }
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCheck,
                modifier = Modifier.weight(1f),
                enabled = !isChecking && localHost.isNotBlank() && localApiKey.isNotBlank()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...")
                } else {
                    Text("Check")
                }
            }
            OutlinedButton(
                onClick = onScanQr,
                enabled = !isChecking
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan QR",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificateAcceptanceDialog(
    certInfo: CertificateInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onReject
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Accept Certificate?",
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Server: ${certInfo.host}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Subject: ${certInfo.subject}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Issuer: ${certInfo.issuer}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Valid: ${certInfo.validFrom} to ${certInfo.validTo}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fingerprint (SHA-256):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        certInfo.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReject) {
                        Text("Reject")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onAccept) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}