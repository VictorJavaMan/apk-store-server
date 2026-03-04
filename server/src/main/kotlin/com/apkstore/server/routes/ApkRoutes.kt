package com.apkstore.server.routes

import com.apkstore.server.database.ApkFileEntity
import com.apkstore.server.database.ApkFiles
import com.apkstore.server.models.ApkInfo
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
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

fun Route.apkRoutes() {
    val uploadDir = File("uploads").also { it.mkdirs() }

    route("/api/apks") {
        // Get all APKs
        get {
            val apps = transaction {
                ApkFileEntity.all().map { it.toApkInfo() }
            }
            call.respond(ApkListResponse(apps = apps, total = apps.size))
        }

        // Get single APK info
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }

            val apk = transaction {
                ApkFileEntity.findById(id)?.toApkInfo()
            }

            if (apk != null) {
                call.respond(apk)
            } else {
                call.respond(HttpStatusCode.NotFound, "APK not found")
            }
        }

        // Upload APK
        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var packageName = "unknown"
            var versionName = "1.0"
            var versionCode = 1
            var description: String? = null
            var savedFile: File? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "packageName" -> packageName = part.value
                            "versionName" -> versionName = part.value
                            "versionCode" -> versionCode = part.value.toIntOrNull() ?: 1
                            "description" -> description = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "${UUID.randomUUID()}.apk"
                        val fileBytes = part.streamProvider().readBytes()
                        savedFile = File(uploadDir, "${UUID.randomUUID()}_$fileName").also {
                            it.writeBytes(fileBytes)
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (savedFile != null && fileName != null) {
                val finalFileName = fileName!!
                val finalSavedFile = savedFile!!
                val apkInfo = transaction {
                    ApkFileEntity.new {
                        this.fileName = finalFileName
                        this.packageName = packageName
                        this.versionName = versionName
                        this.versionCode = versionCode
                        this.fileSize = finalSavedFile.length()
                        this.description = description
                        this.storagePath = finalSavedFile.absolutePath
                        this.uploadedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        this.downloadCount = 0
                    }.toApkInfo()
                }

                call.respond(UploadResponse(
                    success = true,
                    message = "File uploaded successfully",
                    apkInfo = apkInfo
                ))
            } else {
                call.respond(HttpStatusCode.BadRequest, UploadResponse(
                    success = false,
                    message = "No file provided"
                ))
            }
        }

        // Download APK
        get("/{id}/download") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }

            val apkData = transaction {
                ApkFileEntity.findById(id)?.let { entity ->
                    entity.downloadCount++
                    Triple(entity.storagePath, entity.fileName, true)
                }
            }

            if (apkData != null) {
                val (storagePath, apkFileName, _) = apkData
                val file = File(storagePath)
                if (file.exists()) {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            apkFileName
                        ).toString()
                    )
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound, "File not found on server")
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "APK not found")
            }
        }

        // Delete APK
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }

            val deleted = transaction {
                val entity = ApkFileEntity.findById(id)
                if (entity != null) {
                    val file = File(entity.storagePath)
                    file.delete()
                    entity.delete()
                    true
                } else {
                    false
                }
            }

            if (deleted) {
                call.respond(MessageResponse(success = true, message = "APK deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, MessageResponse(success = false, message = "APK not found"))
            }
        }

        // Search APKs
        get("/search/{query}") {
            val query = call.parameters["query"] ?: ""
            val apps = transaction {
                ApkFileEntity.all()
                    .filter {
                        it.fileName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                    }
                    .map { it.toApkInfo() }
            }
            call.respond(ApkListResponse(apps = apps, total = apps.size))
        }
    }
}

private fun ApkFileEntity.toApkInfo() = ApkInfo(
    id = this.id.value,
    fileName = this.fileName,
    packageName = this.packageName,
    versionName = this.versionName,
    versionCode = this.versionCode,
    fileSize = this.fileSize,
    description = this.description,
    uploadedAt = this.uploadedAt.toString(),
    downloadCount = this.downloadCount
)
