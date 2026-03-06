package com.apkstore.server

import com.apkstore.server.database.DatabaseFactory
import com.apkstore.server.plugins.configureRouting
import com.apkstore.server.plugins.configureSerialization
import com.apkstore.server.plugins.configureCors
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.security.KeyStore

fun main() {
    val httpPort = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val httpsPort = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443
    val enableHttps = System.getenv("ENABLE_HTTPS")?.toBoolean() ?: true

    val keyStoreFile = File("keystore.jks")
    val keyStorePassword = System.getenv("KEYSTORE_PASSWORD") ?: "apkstore"
    val keyAlias = "apkstore"

    // Generate self-signed certificate if it doesn't exist
    if (enableHttps && !keyStoreFile.exists()) {
        println("Generating self-signed certificate...")
        generateCertificate(
            file = keyStoreFile,
            keyAlias = keyAlias,
            keyPassword = keyStorePassword,
            jksPassword = keyStorePassword
        )
        println("Certificate generated: ${keyStoreFile.absolutePath}")
    }

    val keyStore: KeyStore? = if (enableHttps && keyStoreFile.exists()) {
        KeyStore.getInstance("JKS").apply {
            keyStoreFile.inputStream().use { load(it, keyStorePassword.toCharArray()) }
        }
    } else null

    val environment = applicationEngineEnvironment {
        connector {
            port = httpPort
            host = "0.0.0.0"
        }
        if (keyStore != null) {
            sslConnector(
                keyStore = keyStore,
                keyAlias = keyAlias,
                keyStorePassword = { keyStorePassword.toCharArray() },
                privateKeyPassword = { keyStorePassword.toCharArray() }
            ) {
                port = httpsPort
                host = "0.0.0.0"
            }
        }
        module(Application::module)
    }

    embeddedServer(Netty, environment).start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureCors()
    configureRouting()
}
