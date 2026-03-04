package com.apkstore.shared.network

import com.apkstore.shared.domain.ApkInfo
import com.apkstore.shared.domain.ApkListResponse
import com.apkstore.shared.domain.UploadResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApkStoreApi(private val baseUrl: String = "http://localhost:8080") {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getApks(): Result<List<ApkInfo>> = runCatching {
        val response: ApkListResponse = client.get("$baseUrl/api/apks").body()
        response.apps
    }

    suspend fun getApk(id: Int): Result<ApkInfo> = runCatching {
        client.get("$baseUrl/api/apks/$id").body()
    }

    suspend fun searchApks(query: String): Result<List<ApkInfo>> = runCatching {
        val response: ApkListResponse = client.get("$baseUrl/api/apks/search/$query").body()
        response.apps
    }

    suspend fun uploadApk(
        fileBytes: ByteArray,
        fileName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        description: String?
    ): Result<UploadResponse> = runCatching {
        client.submitFormWithBinaryData(
            url = "$baseUrl/api/apks/upload",
            formData = formData {
                append("packageName", packageName)
                append("versionName", versionName)
                append("versionCode", versionCode.toString())
                description?.let { append("description", it) }
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ).body()
    }

    suspend fun deleteApk(id: Int): Result<Boolean> = runCatching {
        val response = client.delete("$baseUrl/api/apks/$id")
        response.status == HttpStatusCode.OK
    }

    fun getDownloadUrl(id: Int): String = "$baseUrl/api/apks/$id/download"

    fun close() {
        client.close()
    }
}
