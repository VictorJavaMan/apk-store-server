package com.apkstore.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.apkstore.app.di.appModule
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule("http://localhost:8080"))
    }

    ComposeViewport(document.body!!) {
        App(
            onDownload = { url ->
                window.open(url, "_blank")
            }
        )
    }
}
