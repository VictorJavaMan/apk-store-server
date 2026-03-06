package com.apkstore.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "apk_client_prefs"
private const val KEY_SERVER_URL = "server_url"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientConnectScreen(
    onBack: () -> Unit,
    onConnect: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var serverUrl by remember {
        mutableStateOf(prefs.getString(KEY_SERVER_URL, "") ?: "")
    }
    var isConnecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подключение к серверу") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Введите адрес сервера",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Адрес показан на экране сервера в приложении",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    error = null
                },
                label = { Text("URL сервера") },
                placeholder = { Text("https://192.168.1.100:8443") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (serverUrl.isNotBlank()) {
                            connectToServer(serverUrl, prefs, onConnect) { error = it }
                        }
                    }
                ),
                isError = error != null,
                supportingText = error?.let { { Text(it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (serverUrl.isNotBlank()) {
                        connectToServer(serverUrl, prefs, onConnect) { error = it }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverUrl.isNotBlank() && !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Подключиться")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Примеры:\nhttps://192.168.1.100:8443\nhttp://192.168.1.100:8080",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun connectToServer(
    url: String,
    prefs: android.content.SharedPreferences,
    onConnect: (String) -> Unit,
    onError: (String) -> Unit
) {
    val normalizedUrl = url.trim().trimEnd('/')

    if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
        onError("URL должен начинаться с http:// или https://")
        return
    }

    // Save URL for next time
    prefs.edit().putString(KEY_SERVER_URL, normalizedUrl).apply()

    onConnect(normalizedUrl)
}
