plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation.common)
            implementation(libs.ktor.server.cors.common)
            implementation(libs.ktor.server.partial.content.common)
            implementation(libs.ktor.server.auto.head.response.common)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.exposed.core)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.sqlite.jdbc)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.service)
        }
    }
}

android {
    namespace = "com.apkstore.server"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}
