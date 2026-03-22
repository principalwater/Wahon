package com.wahon.shared.di

import com.russhwolf.settings.Settings
import org.koin.dsl.module

actual val platformModule = module {
    single<Settings> { Settings() }
}
