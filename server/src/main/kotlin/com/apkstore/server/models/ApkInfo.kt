package com.apkstore.server.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ApkInfo(
    val id: Int,
    val fileName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val fileSize: Long,
    val description: String?,
    val uploadedAt: String,
    val downloadCount: Int
)

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val apkInfo: ApkInfo? = null
)

@Serializable
data class ApkListResponse(
    val apps: List<ApkInfo>,
    val total: Int
)

@Serializable
data class MessageResponse(
    val success: Boolean,
    val message: String
)
