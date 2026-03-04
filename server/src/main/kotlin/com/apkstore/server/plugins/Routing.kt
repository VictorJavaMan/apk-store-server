package com.apkstore.server.plugins

import com.apkstore.server.routes.apkRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    install(PartialContent)
    install(AutoHeadResponse)

    routing {
        apkRoutes()

        // Serve web frontend
        get("/") {
            val indexFile = findWebFile("index.html")
            if (indexFile != null && indexFile.exists()) {
                call.respondFile(indexFile)
            } else {
                call.respondText("Web frontend not found. Place index.html in server/web/ folder.", status = HttpStatusCode.NotFound)
            }
        }

        // Serve static files (CSS, JS, images, etc.)
        get("/static/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
            val file = findWebFile("static/$path")
            if (file != null && file.exists() && file.isFile) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Serve any file from web directory
        get("/{file}") {
            val fileName = call.parameters["file"] ?: return@get call.respond(HttpStatusCode.NotFound)
            // Skip API routes
            if (fileName == "api") return@get call.respond(HttpStatusCode.NotFound)

            val file = findWebFile(fileName)
            if (file != null && file.exists() && file.isFile) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

private fun findWebFile(relativePath: String): File? {
    val possiblePaths = listOf(
        "web/$relativePath",           // Docker: /app/web
        "server/web/$relativePath",    // Local dev from project root
        "../web/$relativePath"         // Local dev from server dir
    )
    return possiblePaths.map { File(it) }.firstOrNull { it.exists() }
}
