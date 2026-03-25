package com.wahon.shared.data.remote

import com.russhwolf.settings.Settings

class NetworkPreferencesStore(
    private val settings: Settings,
) {
    private var cachedDohProvider: DnsOverHttpsProvider? = null

    fun selectedDohProvider(): DnsOverHttpsProvider {
        cachedDohProvider?.let { return it }
        val raw = settings.getStringOrNull(DOH_PROVIDER_KEY)
        val resolved = DnsOverHttpsProvider.fromStorageValue(raw)
        cachedDohProvider = resolved
        return resolved
    }

    fun setSelectedDohProvider(provider: DnsOverHttpsProvider) {
        cachedDohProvider = provider
        settings.putString(DOH_PROVIDER_KEY, provider.storageValue)
    }
}

private const val DOH_PROVIDER_KEY = "network.doh_provider"
