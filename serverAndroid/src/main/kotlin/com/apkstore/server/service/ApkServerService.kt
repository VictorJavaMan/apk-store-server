package com.apkstore.server.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.apkstore.server.ApkServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApkServerService : Service() {

    private var server: ApkServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startServer(intent.getIntExtra(EXTRA_PORT, 8080))
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    private fun startServer(port: Int) {
        Log.d(TAG, "startServer: port=$port, isRunning=${server?.isRunning}")
        if (server?.isRunning == true) return

        // Start foreground immediately
        _serverState.value = ServerState(isRunning = false, error = "Starting...")
        startForeground(NOTIFICATION_ID, createNotification())

        acquireWakeLock()

        server = ApkServer(applicationContext, port).apply {
            onStatusChange = { running, urlOrError ->
                Log.d(TAG, "Server status changed: running=$running, url=$urlOrError")
                _serverState.value = if (running) {
                    ServerState(isRunning = true, serverUrl = urlOrError)
                } else {
                    ServerState(isRunning = false, error = urlOrError)
                }
                updateNotification()
            }
            start()
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        releaseWakeLock()
        _serverState.value = ServerState(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ApkServer::WakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 hours max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "APK For Testers Server notifications"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val state = _serverState.value
        val contentText = if (state.isRunning) {
            "Running at ${state.serverUrl}"
        } else {
            "Server stopped"
        }

        val stopIntent = Intent(this, ApkServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("APK For Testers Server")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ApkServerService"
        const val ACTION_START = "com.apkstore.server.START"
        const val ACTION_STOP = "com.apkstore.server.STOP"
        const val EXTRA_PORT = "port"

        private const val CHANNEL_ID = "apk_server_channel"
        private const val NOTIFICATION_ID = 1

        private val _serverState = MutableStateFlow(ServerState())
        val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

        fun startServer(context: Context, port: Int = 8080) {
            Log.d(TAG, "startServer called: port=$port")
            val intent = Intent(context, ApkServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopServer(context: Context) {
            Log.d(TAG, "stopServer called")
            val intent = Intent(context, ApkServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

data class ServerState(
    val isRunning: Boolean = false,
    val serverUrl: String? = null,
    val error: String? = null
)
