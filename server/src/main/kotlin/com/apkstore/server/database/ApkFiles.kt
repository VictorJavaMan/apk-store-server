package com.apkstore.server.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ApkFiles : IntIdTable("apk_files") {
    val fileName = varchar("file_name", 255)
    val packageName = varchar("package_name", 255)
    val versionName = varchar("version_name", 100)
    val versionCode = integer("version_code")
    val fileSize = long("file_size")
    val description = text("description").nullable()
    val storagePath = varchar("storage_path", 500)
    val uploadedAt = datetime("uploaded_at")
    val downloadCount = integer("download_count").default(0)
}

class ApkFileEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ApkFileEntity>(ApkFiles)

    var fileName by ApkFiles.fileName
    var packageName by ApkFiles.packageName
    var versionName by ApkFiles.versionName
    var versionCode by ApkFiles.versionCode
    var fileSize by ApkFiles.fileSize
    var description by ApkFiles.description
    var storagePath by ApkFiles.storagePath
    var uploadedAt by ApkFiles.uploadedAt
    var downloadCount by ApkFiles.downloadCount
}
