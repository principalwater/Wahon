package com.wahon.shared.di

import com.russhwolf.settings.Settings
import com.wahon.shared.data.local.DatabaseDriverFactory
import com.wahon.shared.data.local.ExtensionFileStore
import com.wahon.shared.data.local.OfflineChapterFileStore
import com.wahon.shared.data.remote.AntiBotChallengeResolver
import com.wahon.shared.data.remote.IosWkWebViewAntiBotChallengeResolver
import org.koin.dsl.module

actual val platformModule = module {
    single<Settings> { Settings() }
    single { DatabaseDriverFactory() }
    single { ExtensionFileStore() }
    single { OfflineChapterFileStore() }
    single<AntiBotChallengeResolver> { IosWkWebViewAntiBotChallengeResolver(cookiesStorage = get()) }
}
