package com.apkstore.app.server

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.apkstore.app.ui.ApkStoreViewModel
import com.apkstore.app.ui.screens.ApkListScreen
import com.apkstore.app.ui.theme.ApkStoreTheme
import com.apkstore.server.service.ApkServerService
import org.koin.compose.viewmodel.koinViewModel

enum class AppScreen {
    Apps, Server
}

@Composable
fun ServerApp(
    onDownload: (String) -> Unit = {}
) {
    ApkStoreTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.Apps) }
        var showUploadDialog by remember { mutableStateOf(false) }

        val viewModel: ApkStoreViewModel = koinViewModel()
        val state by viewModel.state.collectAsState()
        val serverState by ApkServerService.serverState.collectAsState()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Apps,
                        onClick = { currentScreen = AppScreen.Apps },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Apps") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Server,
                        onClick = { currentScreen = AppScreen.Server },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Server") }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    AppScreen.Apps -> {
                        ApkListScreen(
                            state = state,
                            onSearch = viewModel::search,
                            onRefresh = viewModel::loadApks,
                            onApkClick = viewModel::selectApk,
                            onApkDismiss = { viewModel.selectApk(null) },
                            onDownload = { id -> onDownload(viewModel.getDownloadUrl(id)) },
                            onDelete = viewModel::deleteApk,
                            onUploadClick = {
                                if (serverState.isRunning && serverState.serverUrl != null) {
                                    showUploadDialog = true
                                }
                            }
                        )
                    }
                    AppScreen.Server -> {
                        ServerControlScreen()
                    }
                }
            }
        }

        // Upload dialog
        if (showUploadDialog && serverState.serverUrl != null) {
            UploadApkDialog(
                serverUrl = serverState.serverUrl!!,
                onDismiss = { showUploadDialog = false },
                onSuccess = {
                    viewModel.loadApks()
                }
            )
        }
    }
}
