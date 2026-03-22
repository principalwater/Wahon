package com.wahon.shared.di

import com.wahon.shared.data.remote.ExtensionRepoApi
import com.wahon.shared.data.remote.createHttpClient
import com.wahon.shared.data.repository.ExtensionRepoRepositoryImpl
import com.wahon.shared.domain.repository.ExtensionRepoRepository
import org.koin.dsl.module

val sharedModule = module {
    single { createHttpClient() }
    single { ExtensionRepoApi(get()) }
    single<ExtensionRepoRepository> { ExtensionRepoRepositoryImpl(get(), get()) }
}
