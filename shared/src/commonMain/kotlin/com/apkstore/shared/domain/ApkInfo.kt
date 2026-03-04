package com.apkstore.shared.domain

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
) {
    val fileSizeFormatted: String
        get() {
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1 -> "%.2f MB".format(mb)
                kb >= 1 -> "%.2f KB".format(kb)
                else -> "$fileSize B"
            }
        }
}

@Serializable
data class ApkListResponse(
    val apps: List<ApkInfo>,
    val total: Int
)

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val apkInfo: ApkInfo? = null
)
