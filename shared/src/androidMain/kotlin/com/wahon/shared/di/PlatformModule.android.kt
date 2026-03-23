package com.wahon.shared.di

import com.russhwolf.settings.Settings
import com.wahon.shared.data.local.DatabaseDriverFactory
import com.wahon.shared.data.local.ExtensionFileStore
import com.wahon.shared.data.local.OfflineChapterFileStore
import com.wahon.shared.data.remote.AndroidWebViewAntiBotChallengeResolver
import com.wahon.shared.data.remote.AntiBotChallengeResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<Settings> { Settings() }
    single { DatabaseDriverFactory(androidContext()) }
    single { ExtensionFileStore(androidContext()) }
    single { OfflineChapterFileStore(androidContext()) }
    single<AntiBotChallengeResolver> {
        AndroidWebViewAntiBotChallengeResolver(
            context = androidContext(),
            cookiesStorage = get()
        )
    }
}
