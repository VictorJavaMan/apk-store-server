package com.apkstore.app.server

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkstore.server.service.ApkServerService
import com.apkstore.server.service.ServerState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ServerControlScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var serverState by remember { mutableStateOf(ServerState()) }
    var port by remember { mutableStateOf("8080") }
    var service by remember { mutableStateOf<ApkServerService?>(null) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as? ApkServerService.LocalBinder)?.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, ApkServerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    LaunchedEffect(service) {
        service?.serverState?.collectLatest { state ->
            serverState = state
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Local Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(50),
                        color = if (serverState.isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ) {}
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (serverState.isRunning) "Running" else "Stopped",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (serverState.isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Server URL
            if (serverState.isRunning && serverState.serverUrl != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(serverState.serverUrl!!))
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Server URL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = serverState.serverUrl!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap to copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message
            serverState.error?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Port input (only when stopped)
            if (!serverState.isRunning) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            port = newValue
                        }
                    },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Start/Stop button
            Button(
                onClick = {
                    if (serverState.isRunning) {
                        ApkServerService.stopServer(context)
                    } else {
                        val portNumber = port.toIntOrNull() ?: 8080
                        ApkServerService.startServer(context, portNumber)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (serverState.isRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (serverState.isRunning) "Stop Server" else "Start Server",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Info text
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Other devices can access this server via the URL above when connected to the same network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
