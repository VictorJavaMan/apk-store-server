package com.apkstore.server.plugins

import android.content.Context
import com.apkstore.server.routes.apkRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(context: Context) {
    install(PartialContent)
    install(AutoHeadResponse)

    routing {
        apkRoutes(context)

        get("/") {
            call.respondText(
                """
                APK Store Server (Android)

                API Endpoints:
                - GET /api/apks - List all APKs
                - GET /api/apks/{id} - Get APK info
                - POST /api/apks/upload - Upload APK
                - GET /api/apks/{id}/download - Download APK
                - DELETE /api/apks/{id} - Delete APK
                - GET /api/apks/search/{query} - Search APKs
                """.trimIndent(),
                ContentType.Text.Plain
            )
        }

        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }
    }
}
