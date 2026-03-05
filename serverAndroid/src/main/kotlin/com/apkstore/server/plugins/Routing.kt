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
            try {
                val html = context.assets.open("web/index.html").bufferedReader().readText()
                call.respondText(html, ContentType.Text.Html)
            } catch (e: Exception) {
                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head><title>APK Store Server</title></head>
                    <body style="font-family: sans-serif; padding: 40px; text-align: center;">
                        <h1>APK Store Server (Android)</h1>
                        <p>Server is running!</p>
                        <h3>API Endpoints:</h3>
                        <ul style="list-style: none; padding: 0;">
                            <li>GET /api/apks - List all APKs</li>
                            <li>GET /api/apks/{id} - Get APK info</li>
                            <li>POST /api/apks/upload - Upload APK</li>
                            <li>GET /api/apks/{id}/download - Download APK</li>
                            <li>DELETE /api/apks/{id} - Delete APK</li>
                        </ul>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }
        }

        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }
    }
}
