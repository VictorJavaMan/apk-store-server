package com.apkstore.app.server

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun UploadApkDialog(
    serverUrl: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = c.getString(nameIndex)
                    }
                }
            }
            if (fileName.isEmpty()) {
                fileName = it.lastPathSegment ?: "app.apk"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload APK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File selection
                OutlinedButton(
                    onClick = { filePicker.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fileName.isEmpty()) "Select APK file" else fileName)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedUri == null) {
                            error = "Please select a file"
                            return@Button
                        }

                        isUploading = true
                        error = null

                        scope.launch {
                            try {
                                val result = uploadFile(
                                    context = context,
                                    serverUrl = serverUrl,
                                    uri = selectedUri!!,
                                    fileName = fileName,
                                    description = description.ifBlank { null }
                                )
                                if (result) {
                                    onSuccess()
                                    onDismiss()
                                } else {
                                    error = "Upload failed"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Upload failed"
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading && selectedUri != null
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isUploading) "Uploading..." else "Upload")
                }
            }
        }
    }
}

private suspend fun uploadFile(
    context: android.content.Context,
    serverUrl: String,
    uri: Uri,
    fileName: String,
    description: String?
): Boolean = withContext(Dispatchers.IO) {
    val boundary = "----${System.currentTimeMillis()}"
    val lineEnd = "\r\n"

    val url = URL("$serverUrl/api/apks/upload")
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.doOutput = true
        connection.doInput = true
        connection.useCaches = false
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val outputStream = connection.outputStream

        // Write form fields
        fun writeField(name: String, value: String) {
            outputStream.write("--$boundary$lineEnd".toByteArray())
            outputStream.write("Content-Disposition: form-data; name=\"$name\"$lineEnd".toByteArray())
            outputStream.write(lineEnd.toByteArray())
            outputStream.write("$value$lineEnd".toByteArray())
        }

        description?.let { writeField("description", it) }

        // Write file
        val inputStream = context.contentResolver.openInputStream(uri)
        val uploadFileName = fileName.ifEmpty { "app.apk" }

        outputStream.write("--$boundary$lineEnd".toByteArray())
        outputStream.write("Content-Disposition: form-data; name=\"file\"; filename=\"$uploadFileName\"$lineEnd".toByteArray())
        outputStream.write("Content-Type: application/vnd.android.package-archive$lineEnd".toByteArray())
        outputStream.write(lineEnd.toByteArray())

        inputStream?.copyTo(outputStream)
        inputStream?.close()

        outputStream.write(lineEnd.toByteArray())
        outputStream.write("--$boundary--$lineEnd".toByteArray())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            val errorStream = connection.errorStream
            val errorBody = errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("Server error $responseCode: $errorBody")
        }
        true
    } finally {
        connection.disconnect()
    }
}
