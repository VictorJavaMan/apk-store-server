package com.apkstore.server.routes

import android.content.Context
import android.util.Log
import com.apkstore.server.database.DatabaseHelper
import com.apkstore.server.models.ApkListResponse
import com.apkstore.server.models.MessageResponse
import com.apkstore.server.models.UploadResponse
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.util.*

private const val TAG = "ApkRoutes"

fun Route.apkRoutes(context: Context) {
    val uploadDir = File(context.filesDir, "uploads").also { it.mkdirs() }
    val db = DatabaseHelper.getInstance(context)

    route("/api/apks") {
        // Get all APKs
        get {
            Log.d(TAG, "GET /api/apks")
            try {
                val apps = db.getAllApks()
                Log.d(TAG, "Found ${apps.size} apps")
                call.respond(ApkListResponse(apps = apps, total = apps.size))
            } catch (e: Exception) {
                Log.e(TAG, "Error getting apks", e)
                call.respond(HttpStatusCode.InternalServerError, MessageResponse(false, e.message ?: "Unknown error"))
            }
        }

        // Get single APK info
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Invalid ID"))
                return@get
            }

            val apk = db.getApkById(id)
            if (apk != null) {
                call.respond(apk)
            } else {
                call.respond(HttpStatusCode.NotFound, MessageResponse(false, "APK not found"))
            }
        }

        // Upload APK
        post("/upload") {
            Log.d(TAG, "POST /api/apks/upload")
            try {
                val multipart = call.receiveMultipart()
                var fileName: String? = null
                var description: String? = null
                var savedFile: File? = null

                multipart.forEachPart { part ->
                    Log.d(TAG, "Processing part: ${part.name}, type: ${part::class.simpleName}")
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "description" -> description = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            fileName = part.originalFileName ?: "${UUID.randomUUID()}.apk"
                            Log.d(TAG, "Receiving file: $fileName")

                            // Stream file directly to disk without loading into memory
                            val targetFile = File(uploadDir, "${UUID.randomUUID()}_$fileName")
                            targetFile.outputStream().buffered().use { output ->
                                part.streamProvider().use { input ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    var totalBytes = 0L
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        totalBytes += bytesRead
                                    }
                                    Log.d(TAG, "File streamed: $totalBytes bytes")
                                }
                            }
                            savedFile = targetFile
                            Log.d(TAG, "File saved to: ${savedFile?.absolutePath}")
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (savedFile != null && fileName != null) {
                    val uploadedAt = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString()

                    Log.d(TAG, "Inserting into database: $fileName")
                    val id = db.insertApk(
                        fileName = fileName!!,
                        fileSize = savedFile!!.length(),
                        description = description,
                        storagePath = savedFile!!.absolutePath,
                        uploadedAt = uploadedAt
                    )
                    Log.d(TAG, "Inserted with id: $id")

                    val apkInfo = db.getApkById(id.toInt())

                    call.respond(UploadResponse(
                        success = true,
                        message = "File uploaded successfully",
                        apkInfo = apkInfo
                    ))
                } else {
                    Log.w(TAG, "No file received: fileName=$fileName, savedFile=$savedFile")
                    call.respond(HttpStatusCode.BadRequest, UploadResponse(
                        success = false,
                        message = "No file provided"
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                call.respond(HttpStatusCode.InternalServerError, UploadResponse(
                    success = false,
                    message = "Upload error: ${e.message}"
                ))
            }
        }

        // Download APK
        get("/{id}/download") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Invalid ID"))
                return@get
            }

            val pathInfo = db.getApkStoragePath(id)
            if (pathInfo != null) {
                val (storagePath, apkFileName) = pathInfo
                val file = File(storagePath)
                if (file.exists()) {
                    db.incrementDownloadCount(id)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            apkFileName
                        ).toString()
                    )
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound, MessageResponse(false, "File not found on server"))
                }
            } else {
                call.respond(HttpStatusCode.NotFound, MessageResponse(false, "APK not found"))
            }
        }

        // Delete APK
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Invalid ID"))
                return@delete
            }

            val pathInfo = db.getApkStoragePath(id)
            if (pathInfo != null) {
                val (storagePath, _) = pathInfo
                File(storagePath).delete()
                db.deleteApk(id)
                call.respond(MessageResponse(success = true, message = "APK deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, MessageResponse(success = false, message = "APK not found"))
            }
        }

        // Search APKs
        get("/search/{query}") {
            val query = call.parameters["query"] ?: ""
            val apps = db.searchApks(query)
            call.respond(ApkListResponse(apps = apps, total = apps.size))
        }
    }
}
