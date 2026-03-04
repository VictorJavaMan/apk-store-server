package com.apkstore.shared.di

import com.apkstore.shared.data.ApkRepository
import com.apkstore.shared.network.ApkStoreApi
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun sharedModule(baseUrl: String = "http://localhost:8080") = module {
    single { ApkStoreApi(baseUrl) }
    singleOf(::ApkRepository)
}
