package com.apkstore.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.apkstore.app.ui.ApkStoreViewModel
import com.apkstore.app.ui.screens.ApkListScreen
import com.apkstore.app.ui.theme.ApkStoreTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    onDownload: (String) -> Unit = {}
) {
    ApkStoreTheme {
        val viewModel: ApkStoreViewModel = koinViewModel()
        val state by viewModel.state.collectAsState()

        ApkListScreen(
            state = state,
            onSearch = viewModel::search,
            onRefresh = viewModel::loadApks,
            onApkClick = viewModel::selectApk,
            onApkDismiss = { viewModel.selectApk(null) },
            onDownload = { id -> onDownload(viewModel.getDownloadUrl(id)) },
            onDelete = viewModel::deleteApk,
            onUploadClick = { viewModel.showUploadDialog(true) }
        )
    }
}
