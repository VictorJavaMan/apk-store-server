package com.apkstore.shared

actual fun getPlatform(): Platform = JsPlatform()

class JsPlatform : Platform {
    override val name: String = "Web"
}
