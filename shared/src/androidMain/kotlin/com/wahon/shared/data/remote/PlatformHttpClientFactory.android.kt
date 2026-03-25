package com.wahon.shared.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.net.InetAddress
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

actual fun createPlatformHttpClient(
    dohProviderResolver: () -> DnsOverHttpsProvider,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    val dnsResolver = DynamicDohDnsResolver(dohProviderResolver)
    return HttpClient(OkHttp) {
        engine {
            config {
                dns(dnsResolver)
            }
        }
        configure()
    }
}

private class DynamicDohDnsResolver(
    private val dohProviderResolver: () -> DnsOverHttpsProvider,
) : Dns {
    private val resolverCache = mutableMapOf<DnsOverHttpsProvider, Dns>()

    @Volatile
    private var lastObservedProvider: DnsOverHttpsProvider = DnsOverHttpsProvider.DISABLED

    override fun lookup(hostname: String): List<InetAddress> {
        val provider = runCatching(dohProviderResolver)
            .getOrDefault(DnsOverHttpsProvider.DISABLED)
        maybeLogProviderChange(provider)

        if (provider == DnsOverHttpsProvider.DISABLED) {
            return Dns.SYSTEM.lookup(hostname)
        }

        val resolver = synchronized(this) {
            resolverCache.getOrPut(provider) { createDnsResolver(provider) }
        }
        return runCatching {
            resolver.lookup(hostname)
        }.onFailure { error ->
            Napier.w(
                message = "DoH lookup failed for $hostname with ${provider.storageValue}, fallback to system DNS: ${error.message.orEmpty()}",
                tag = LOG_TAG,
            )
        }.getOrElse {
            Dns.SYSTEM.lookup(hostname)
        }
    }

    private fun maybeLogProviderChange(provider: DnsOverHttpsProvider) {
        val shouldLog = synchronized(this) {
            if (provider == lastObservedProvider) {
                false
            } else {
                lastObservedProvider = provider
                true
            }
        }
        if (!shouldLog) return

        if (provider == DnsOverHttpsProvider.DISABLED) {
            Napier.i(
                message = "DNS-over-HTTPS disabled. Using system DNS.",
                tag = LOG_TAG,
            )
        } else {
            Napier.i(
                message = "DNS-over-HTTPS provider switched to ${provider.storageValue}.",
                tag = LOG_TAG,
            )
        }
    }
}

private fun createDnsResolver(provider: DnsOverHttpsProvider): Dns {
    if (provider == DnsOverHttpsProvider.DISABLED) return Dns.SYSTEM
    val endpoint = provider.endpointUrl
    if (endpoint.isNullOrBlank()) return Dns.SYSTEM

    return runCatching {
        val bootstrapHosts = provider.bootstrapHosts.map { host ->
            InetAddress.getByName(host)
        }
        DnsOverHttps.Builder()
            .client(
                OkHttpClient.Builder()
                    .dns(Dns.SYSTEM)
                    .build(),
            )
            .url(endpoint.toHttpUrl())
            .bootstrapDnsHosts(bootstrapHosts)
            .resolvePrivateAddresses(false)
            .resolvePublicAddresses(true)
            .build()
    }.onSuccess {
        Napier.i(
            message = "DNS-over-HTTPS enabled: ${provider.storageValue}",
            tag = LOG_TAG,
        )
    }.onFailure { error ->
        Napier.w(
            message = "Failed to initialize DoH (${provider.storageValue}), fallback to system DNS: ${error.message.orEmpty()}",
            tag = LOG_TAG,
        )
    }.getOrElse { Dns.SYSTEM }
}

private const val LOG_TAG = "PlatformHttpClientFactory"
