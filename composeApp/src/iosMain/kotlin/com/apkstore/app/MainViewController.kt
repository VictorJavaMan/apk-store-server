package com.apkstore.app

import androidx.compose.ui.window.ComposeUIViewController
import com.apkstore.app.di.appModule
import org.koin.core.context.startKoin
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(appModule("http://localhost:8080"))
        }
    }
) {
    App(
        onDownload = { url ->
            NSURL.URLWithString(url)?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(nsUrl)
            }
        }
    )
}
