package com.apkstore.server.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.apkstore.server.models.ApkInfo

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_APK_FILES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FILE_NAME TEXT NOT NULL,
                $COLUMN_PACKAGE_NAME TEXT NOT NULL,
                $COLUMN_VERSION_NAME TEXT NOT NULL,
                $COLUMN_VERSION_CODE INTEGER NOT NULL,
                $COLUMN_FILE_SIZE INTEGER NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_STORAGE_PATH TEXT NOT NULL,
                $COLUMN_UPLOADED_AT TEXT NOT NULL,
                $COLUMN_DOWNLOAD_COUNT INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APK_FILES")
        onCreate(db)
    }

    fun insertApk(
        fileName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        fileSize: Long,
        description: String?,
        storagePath: String,
        uploadedAt: String
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_FILE_NAME, fileName)
            put(COLUMN_PACKAGE_NAME, packageName)
            put(COLUMN_VERSION_NAME, versionName)
            put(COLUMN_VERSION_CODE, versionCode)
            put(COLUMN_FILE_SIZE, fileSize)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_STORAGE_PATH, storagePath)
            put(COLUMN_UPLOADED_AT, uploadedAt)
            put(COLUMN_DOWNLOAD_COUNT, 0)
        }
        return writableDatabase.insert(TABLE_APK_FILES, null, values)
    }

    fun getAllApks(): List<ApkInfo> {
        val apks = mutableListOf<ApkInfo>()
        val cursor = readableDatabase.query(
            TABLE_APK_FILES, null, null, null, null, null,
            "$COLUMN_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                apks.add(cursorToApkInfo(it))
            }
        }
        return apks
    }

    fun getApkById(id: Int): ApkInfo? {
        val cursor = readableDatabase.query(
            TABLE_APK_FILES, null,
            "$COLUMN_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToApkInfo(it) else null
        }
    }

    fun getApkStoragePath(id: Int): Pair<String, String>? {
        val cursor = readableDatabase.query(
            TABLE_APK_FILES,
            arrayOf(COLUMN_STORAGE_PATH, COLUMN_FILE_NAME),
            "$COLUMN_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                Pair(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_STORAGE_PATH)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_FILE_NAME))
                )
            } else null
        }
    }

    fun incrementDownloadCount(id: Int) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_APK_FILES SET $COLUMN_DOWNLOAD_COUNT = $COLUMN_DOWNLOAD_COUNT + 1 WHERE $COLUMN_ID = ?",
            arrayOf(id)
        )
    }

    fun deleteApk(id: Int): Boolean {
        return writableDatabase.delete(
            TABLE_APK_FILES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun searchApks(query: String): List<ApkInfo> {
        val apks = mutableListOf<ApkInfo>()
        val searchPattern = "%$query%"
        val cursor = readableDatabase.query(
            TABLE_APK_FILES, null,
            "$COLUMN_FILE_NAME LIKE ? OR $COLUMN_PACKAGE_NAME LIKE ?",
            arrayOf(searchPattern, searchPattern),
            null, null, "$COLUMN_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                apks.add(cursorToApkInfo(it))
            }
        }
        return apks
    }

    private fun cursorToApkInfo(cursor: Cursor): ApkInfo {
        return ApkInfo(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
            packageName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME)),
            versionName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERSION_NAME)),
            versionCode = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VERSION_CODE)),
            fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
            uploadedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLOADED_AT)),
            downloadCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_COUNT))
        )
    }

    companion object {
        private const val DATABASE_NAME = "apkstore.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_APK_FILES = "apk_files"
        const val COLUMN_ID = "id"
        const val COLUMN_FILE_NAME = "file_name"
        const val COLUMN_PACKAGE_NAME = "package_name"
        const val COLUMN_VERSION_NAME = "version_name"
        const val COLUMN_VERSION_CODE = "version_code"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_STORAGE_PATH = "storage_path"
        const val COLUMN_UPLOADED_AT = "uploaded_at"
        const val COLUMN_DOWNLOAD_COUNT = "download_count"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
