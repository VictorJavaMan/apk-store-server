package com.apkstore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apkstore.app.ui.ApkStoreState
import com.apkstore.app.ui.components.ApkDetailSheet
import com.apkstore.app.ui.components.ApkListItem
import com.apkstore.shared.domain.ApkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkListScreen(
    state: ApkStoreState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onApkClick: (ApkInfo) -> Unit,
    onApkDismiss: () -> Unit,
    onDownload: (Int) -> Unit,
    onDelete: ((Int) -> Unit)?,
    onUploadClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var searchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            placeholder = { Text("Search apps...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    } else {
                        Text("APK For Testers")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchExpanded && state.searchQuery.isNotEmpty()) {
                            onSearch("")
                        }
                        searchExpanded = !searchExpanded
                    }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchExpanded) "Close search" else "Search"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (onUploadClick != null) {
                ExtendedFloatingActionButton(
                    onClick = onUploadClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Upload APK") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.apks.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.error != null && state.apks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }

                state.apks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No APKs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload your first APK to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.apks) { apk ->
                            ApkListItem(
                                apk = apk,
                                onClick = { onApkClick(apk) }
                            )
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }

            // Loading overlay
            if (state.isLoading && state.apks.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        // Detail bottom sheet
        state.selectedApk?.let { apk ->
            ApkDetailSheet(
                apk = apk,
                onDismiss = onApkDismiss,
                onDownload = { onDownload(apk.id) },
                onDelete = onDelete?.let { { it(apk.id) } }
            )
        }
    }
}
