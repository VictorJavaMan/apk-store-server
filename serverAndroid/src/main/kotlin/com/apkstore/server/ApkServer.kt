package com.apkstore.server

import android.content.Context
import android.util.Log
import com.apkstore.server.plugins.configureCors
import com.apkstore.server.plugins.configureRouting
import com.apkstore.server.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ApkServer(
    private val context: Context,
    private val port: Int = 8080
) {
    private var server: CIOApplicationEngine? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRunning: Boolean = false
        private set

    var onStatusChange: ((Boolean, String?) -> Unit)? = null

    fun start() {
        if (isRunning) return

        serverJob = scope.launch {
            try {
                Log.d("ApkServer", "Starting server on port $port")

                server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    module(context)
                }.apply {
                    start(wait = false)
                }

                isRunning = true
                Log.d("ApkServer", "Server started at ${getServerUrl()}")
                onStatusChange?.invoke(true, getServerUrl())
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
        onStatusChange?.invoke(false, null)
    }

    fun getServerUrl(): String {
        return "http://${getLocalIpAddress()}:$port"
    }

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
