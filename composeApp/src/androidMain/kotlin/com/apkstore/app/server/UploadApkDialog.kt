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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Get file size for content length
    val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
        it.length
    } ?: -1L

    // Create streaming request body
    val fileBody = object : RequestBody() {
        override fun contentType() = "application/vnd.android.package-archive".toMediaType()

        override fun contentLength() = fileSize

        override fun writeTo(sink: BufferedSink) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                sink.writeAll(inputStream.source())
            }
        }
    }

    val multipartBuilder = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, fileBody)

    description?.let {
        multipartBuilder.addFormDataPart("description", it)
    }

    val request = Request.Builder()
        .url("$serverUrl/api/apks/upload")
        .post(multipartBuilder.build())
        .build()

    val response = client.newCall(request).execute()

    if (!response.isSuccessful) {
        val body = response.body?.string() ?: "Unknown error"
        throw Exception("Server error ${response.code}: $body")
    }

    val body = response.body?.string() ?: "{}"
    val json = JSONObject(body)
    json.optBoolean("success", false)
}
