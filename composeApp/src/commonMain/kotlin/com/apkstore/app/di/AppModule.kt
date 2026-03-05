package com.apkstore.app.di

import com.apkstore.app.ui.ApkStoreViewModel
import com.apkstore.shared.di.sharedModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun appModule(baseUrl: String = "http://localhost:8080") = module {
    includes(sharedModule(baseUrl))
    viewModelOf(::ApkStoreViewModel)
}
