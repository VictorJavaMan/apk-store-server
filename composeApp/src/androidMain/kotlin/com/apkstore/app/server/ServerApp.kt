package com.apkstore.app.server

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.apkstore.app.ui.ApkStoreViewModel
import com.apkstore.app.ui.AppMode
import com.apkstore.app.ui.ClientConnectScreen
import com.apkstore.app.ui.ModeSelectionScreen
import com.apkstore.app.ui.screens.ApkListScreen
import com.apkstore.app.ui.theme.ApkStoreTheme
import com.apkstore.server.service.ApkServerService
import com.apkstore.shared.network.ApkStoreApi
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class AppScreen {
    ModeSelection, ServerMode, ClientConnect, ClientBrowse
}

@Composable
fun ServerApp(
    onDownload: (String) -> Unit = {}
) {
    ApkStoreTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.ModeSelection) }
        var serverTab by remember { mutableStateOf(0) } // 0 = Apps, 1 = Server
        var showUploadDialog by remember { mutableStateOf(false) }
        var clientServerUrl by remember { mutableStateOf("") }

        val viewModel: ApkStoreViewModel = koinViewModel()
        val api: ApkStoreApi = koinInject()
        val state by viewModel.state.collectAsState()
        val serverState by ApkServerService.serverState.collectAsState()

        when (currentScreen) {
            AppScreen.ModeSelection -> {
                ModeSelectionScreen(
                    onModeSelected = { mode ->
                        when (mode) {
                            AppMode.Server -> {
                                api.setServerUrl("http://127.0.0.1:8080")
                                viewModel.loadApks()
                                currentScreen = AppScreen.ServerMode
                            }
                            AppMode.Client -> {
                                currentScreen = AppScreen.ClientConnect
                            }
                        }
                    }
                )
            }

            AppScreen.ServerMode -> {
                ServerModeContent(
                    serverTab = serverTab,
                    onTabChange = { serverTab = it },
                    state = state,
                    serverState = serverState,
                    viewModel = viewModel,
                    onDownload = onDownload,
                    onUploadClick = {
                        if (serverState.isRunning && serverState.serverUrl != null) {
                            showUploadDialog = true
                        }
                    },
                    onBack = { currentScreen = AppScreen.ModeSelection }
                )

                if (showUploadDialog && serverState.serverUrl != null) {
                    // Use localhost for uploads from same device (faster and avoids SSL issues)
                    UploadApkDialog(
                        serverUrl = "http://127.0.0.1:8080",
                        onDismiss = { showUploadDialog = false },
                        onSuccess = { viewModel.loadApks() }
                    )
                }
            }

            AppScreen.ClientConnect -> {
                ClientConnectScreen(
                    onBack = { currentScreen = AppScreen.ModeSelection },
                    onConnect = { url ->
                        clientServerUrl = url
                        api.setServerUrl(url)
                        viewModel.loadApks()
                        currentScreen = AppScreen.ClientBrowse
                    }
                )
            }

            AppScreen.ClientBrowse -> {
                ClientBrowseContent(
                    serverUrl = clientServerUrl,
                    state = state,
                    viewModel = viewModel,
                    onDownload = onDownload,
                    onBack = { currentScreen = AppScreen.ClientConnect },
                    onDisconnect = { currentScreen = AppScreen.ModeSelection }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerModeContent(
    serverTab: Int,
    onTabChange: (Int) -> Unit,
    state: com.apkstore.app.ui.ApkStoreState,
    serverState: com.apkstore.server.service.ServerState,
    viewModel: ApkStoreViewModel,
    onDownload: (String) -> Unit,
    onUploadClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Режим сервера") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = serverTab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Файлы") }
                )
                NavigationBarItem(
                    selected = serverTab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Сервер") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (serverTab) {
                0 -> {
                    ApkListScreen(
                        state = state,
                        onSearch = viewModel::search,
                        onRefresh = viewModel::loadApks,
                        onApkClick = viewModel::selectApk,
                        onApkDismiss = { viewModel.selectApk(null) },
                        onDownload = { id -> onDownload(viewModel.getDownloadUrl(id)) },
                        onDelete = viewModel::deleteApk,
                        onUploadClick = onUploadClick
                    )
                }
                1 -> {
                    ServerControlScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientBrowseContent(
    serverUrl: String,
    state: com.apkstore.app.ui.ApkStoreState,
    viewModel: ApkStoreViewModel,
    onDownload: (String) -> Unit,
    onBack: () -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Клиент")
                        Text(
                            text = serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ApkListScreen(
                state = state,
                onSearch = viewModel::search,
                onRefresh = viewModel::loadApks,
                onApkClick = viewModel::selectApk,
                onApkDismiss = { viewModel.selectApk(null) },
                onDownload = { id -> onDownload(viewModel.getDownloadUrl(id)) },
                onDelete = null, // Client can't delete
                onUploadClick = null // Client can't upload
            )
        }
    }
}
