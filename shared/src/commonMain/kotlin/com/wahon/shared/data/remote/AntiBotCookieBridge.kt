package com.wahon.shared.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url

internal const val ANTI_BOT_RESOLVE_TIMEOUT_MS = 30_000L
internal const val ANTI_BOT_COOKIE_POLL_INTERVAL_MS = 500L

internal fun AntiBotChallenge.expectedCookieNames(): Set<String> = when (protection) {
    AntiBotProtection.CLOUDFLARE -> setOf("cf_clearance")
    AntiBotProtection.DDOS_GUARD -> setOf("__ddg2", "__ddgid", "ddg_clearance", "ddg-clearance")
}

internal fun hasExpectedCookie(
    cookieHeader: String,
    expectedCookieNames: Set<String>,
): Boolean {
    if (cookieHeader.isBlank() || expectedCookieNames.isEmpty()) return false
    val names = cookieHeader.split(';')
        .mapNotNull { token ->
            val separator = token.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            token.substring(0, separator).trim().lowercase().ifBlank { null }
        }
        .toSet()
    return expectedCookieNames.any { expected -> names.contains(expected.lowercase()) }
}

internal suspend fun persistCookieHeader(
    requestUrl: Url,
    cookieHeader: String,
    cookiesStorage: CookiesStorage,
    logTag: String,
): Int {
    var persistedCount = 0
    parseCookieHeader(cookieHeader).forEach { (name, value) ->
        runCatching {
            cookiesStorage.addCookie(
                requestUrl = requestUrl,
                cookie = Cookie(name = name, value = value),
            )
        }.onSuccess {
            persistedCount += 1
        }.onFailure { error ->
            Napier.w(
                message = "Failed to persist challenge cookie $name for $requestUrl: ${error.message.orEmpty()}",
                tag = logTag,
            )
        }
    }
    return persistedCount
}

private fun parseCookieHeader(cookieHeader: String): List<Pair<String, String>> {
    return cookieHeader.split(';')
        .mapNotNull { token ->
            val entry = token.trim()
            if (entry.isBlank()) return@mapNotNull null
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = entry.substring(0, separator).trim()
            val value = entry.substring(separator + 1).trim()
            if (name.isBlank()) return@mapNotNull null
            name to value
        }
}
