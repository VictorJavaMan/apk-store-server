package com.apkstore.server

import android.content.Context
import android.util.Log
import com.apkstore.server.plugins.configureCors
import com.apkstore.server.plugins.configureRouting
import com.apkstore.server.plugins.configureSerialization
import com.apkstore.server.ssl.CertificateManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ApkServer(
    private val context: Context,
    private val httpPort: Int = 8080,
    private val httpsPort: Int = 8443
) {
    private var server: NettyApplicationEngine? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val keyStorePassword = "apkstore"
    private val keyAlias = "apkstore"

    var isRunning: Boolean = false
        private set

    var httpsAvailable: Boolean = false
        private set

    var onStatusChange: ((Boolean, String?) -> Unit)? = null

    fun start() {
        if (isRunning) return

        serverJob = scope.launch {
            try {
                Log.d("ApkServer", "Starting server...")

                // Load SSL keystore from assets
                val keyStore = try {
                    CertificateManager.getKeyStore(context, keyStorePassword)
                } catch (e: Exception) {
                    Log.w("ApkServer", "Failed to load SSL keystore, HTTPS disabled", e)
                    null
                }

                httpsAvailable = keyStore != null

                val environment = applicationEngineEnvironment {
                    // Always have HTTP
                    connector {
                        port = httpPort
                        host = "0.0.0.0"
                    }

                    // Add HTTPS if keystore available
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

                    module { module(context) }
                }

                server = embeddedServer(Netty, environment).apply {
                    start(wait = false)
                }

                isRunning = true
                val url = getServerUrl()
                Log.d("ApkServer", "Server started at $url (HTTPS: $httpsAvailable)")
                onStatusChange?.invoke(true, url)
            } catch (e: Exception) {
                Log.e("ApkServer", "Failed to start server", e)
                isRunning = false
                onStatusChange?.invoke(false, e.message)
            }
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        serverJob?.cancel()
        serverJob = null
        isRunning = false
        httpsAvailable = false
        onStatusChange?.invoke(false, null)
    }

    fun getServerUrl(): String {
        val ip = getLocalIpAddress()
        return if (httpsAvailable) {
            "https://$ip:$httpsPort"
        } else {
            "http://$ip:$httpPort"
        }
    }

    fun getHttpUrl(): String = "http://${getLocalIpAddress()}:$httpPort"
    fun getHttpsUrl(): String = "https://${getLocalIpAddress()}:$httpsPort"

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "localhost"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return "localhost"
    }
}

private fun Application.module(context: Context) {
    configureSerialization()
    configureCors()
    configureRouting(context)
}
