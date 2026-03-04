package com.apkstore.server

import com.apkstore.server.database.DatabaseFactory
import com.apkstore.server.plugins.configureRouting
import com.apkstore.server.plugins.configureSerialization
import com.apkstore.server.plugins.configureCors
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureCors()
    configureRouting()
}
