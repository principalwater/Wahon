package com.wahon.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import com.wahon.app.ui.screen.home.HomeScreen
import com.wahon.app.ui.theme.WahonTheme
import com.wahon.shared.domain.repository.ExtensionRuntimeRepository
import com.wahon.shared.domain.repository.OfflineDownloadRepository
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

@Composable
fun App() {
    val extensionRuntimeRepository = koinInject<ExtensionRuntimeRepository>()
    val offlineDownloadRepository = koinInject<OfflineDownloadRepository>()

    LaunchedEffect(Unit) {
        extensionRuntimeRepository.reloadInstalledSources()
            .onFailure { error ->
                Napier.e("Failed to load installed sources", error)
            }
            .onSuccess {
                offlineDownloadRepository.runAutoDownloadForLibrary()
                    .onFailure { error ->
                        Napier.e("Auto-download sync failed", error)
                    }
            }
    }

    WahonTheme {
        Navigator(HomeScreen())
    }
}
