package com.wahon.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun createPlatformHttpClient(
    dohProviderResolver: () -> DnsOverHttpsProvider,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient
