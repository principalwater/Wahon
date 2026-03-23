package com.wahon.shared.data.remote

import com.russhwolf.settings.Settings
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentCookiesStorage(
    private val settings: Settings,
    private val storageKey: String = DEFAULT_STORAGE_KEY,
    private val maxCookieCount: Int = DEFAULT_MAX_COOKIE_COUNT,
    private val clock: () -> Long = ::currentTimeMillis,
) : CookiesStorage {

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val cookies = mutableListOf<PersistedCookie>()

    init {
        val now = clock()
        val persistedCookies = loadPersistedCookies()
        val loadedCookies = persistedCookies
            .asSequence()
            .filterNot { cookie -> cookie.isExpiredAt(now) }
            .sortedByDescending { cookie -> cookie.updatedAtMs }
            .take(maxCookieCount)
            .toList()

        cookies += loadedCookies
        if (loadedCookies.size != persistedCookies.size) {
            persistCookiesLocked()
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            val now = clock()
            val existing = cookies.firstOrNull { saved ->
                saved.identityMatches(cookie.name, requestUrl, cookie)
            }
            val normalized = PersistedCookie.from(
                cookie = cookie,
                requestUrl = requestUrl,
                nowMs = now,
                createdAtMs = existing?.createdAtMs ?: now,
            )
            cookies.removeAll { saved ->
                saved.identityMatches(normalized.name, normalized.domain, normalized.path)
            }
            if (!normalized.isExpiredAt(now)) {
                cookies += normalized
            }
            purgeExpiredLocked(now)
            trimToLimitLocked()
            persistCookiesLocked()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return mutex.withLock {
            val now = clock()
            val host = requestUrl.host.lowercase()
            val path = normalizePath(requestUrl.encodedPath)
            val isSecureRequest = requestUrl.protocol.name.equals("https", ignoreCase = true) ||
                requestUrl.protocol.name.equals("wss", ignoreCase = true)

            val hadExpired = purgeExpiredLocked(now)
            val matched = cookies
                .asSequence()
                .filter { cookie -> cookie.matches(host, path, isSecureRequest) }
                .map { cookie -> cookie.toCookie() }
                .toList()

            if (hadExpired) {
                persistCookiesLocked()
            }
            matched
        }
    }

    override fun close() = Unit

    private fun loadPersistedCookies(): List<PersistedCookie> {
        val raw = settings.getStringOrNull(storageKey) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PersistedCookie>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun persistCookiesLocked() {
        if (cookies.isEmpty()) {
            settings.remove(storageKey)
            return
        }

        runCatching {
            settings.putString(storageKey, json.encodeToString(cookies))
        }
    }

    private fun purgeExpiredLocked(nowMs: Long): Boolean {
        return cookies.removeAll { cookie -> cookie.isExpiredAt(nowMs) }
    }

    private fun trimToLimitLocked() {
        if (cookies.size <= maxCookieCount) return
        cookies.sortByDescending { cookie -> cookie.updatedAtMs }
        cookies.subList(maxCookieCount, cookies.size).clear()
    }
}

@Serializable
private data class PersistedCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val hostOnly: Boolean,
    val secure: Boolean,
    val httpOnly: Boolean,
    val expiresAtMs: Long?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    fun toCookie(): Cookie {
        return Cookie(
            name = name,
            value = value,
            domain = if (hostOnly) null else domain,
            path = path,
            secure = secure,
            httpOnly = httpOnly,
            expires = expiresAtMs?.let(::GMTDate),
        )
    }

    fun isExpiredAt(nowMs: Long): Boolean {
        val expires = expiresAtMs ?: return false
        return nowMs >= expires
    }

    fun matches(host: String, requestPath: String, isSecureRequest: Boolean): Boolean {
        if (secure && !isSecureRequest) return false
        if (!matchesDomain(host)) return false
        return matchesPath(requestPath)
    }

    fun identityMatches(cookieName: String, requestUrl: Url, cookie: Cookie): Boolean {
        val domain = normalizeDomain(cookie.domain) ?: requestUrl.host.lowercase()
        val path = normalizeCookiePath(cookie.path, requestUrl.encodedPath)
        return identityMatches(cookieName, domain, path)
    }

    fun identityMatches(cookieName: String, cookieDomain: String, cookiePath: String): Boolean {
        return name == cookieName && domain == cookieDomain && path == cookiePath
    }

    private fun matchesDomain(host: String): Boolean {
        return if (hostOnly) {
            host == domain
        } else {
            host == domain || host.endsWith(".$domain")
        }
    }

    private fun matchesPath(requestPath: String): Boolean {
        if (path == "/") return true
        if (requestPath == path) return true
        if (!requestPath.startsWith(path)) return false
        return path.endsWith("/") || requestPath.getOrNull(path.length) == '/'
    }

    companion object {
        fun from(
            cookie: Cookie,
            requestUrl: Url,
            nowMs: Long,
            createdAtMs: Long,
        ): PersistedCookie {
            val hostOnly = cookie.domain.isNullOrBlank()
            val domain = normalizeDomain(cookie.domain) ?: requestUrl.host.lowercase()
            val path = normalizeCookiePath(cookie.path, requestUrl.encodedPath)
            val expiresAtMs = computeExpirationMillis(cookie, nowMs)

            return PersistedCookie(
                name = cookie.name,
                value = cookie.value,
                domain = domain,
                path = path,
                hostOnly = hostOnly,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                expiresAtMs = expiresAtMs,
                createdAtMs = createdAtMs,
                updatedAtMs = nowMs,
            )
        }
    }
}

private fun normalizeDomain(rawDomain: String?): String? {
    val trimmed = rawDomain?.trim()?.trim('.')?.lowercase()
    return trimmed?.takeIf { it.isNotBlank() }
}

private fun normalizeCookiePath(rawPath: String?, requestPath: String): String {
    val explicitPath = rawPath?.trim()?.takeIf { it.isNotEmpty() }?.let(::normalizePath)
    return explicitPath ?: defaultCookiePath(requestPath)
}

private fun normalizePath(path: String): String {
    if (path.isBlank()) return "/"
    return if (path.startsWith("/")) path else "/$path"
}

private fun defaultCookiePath(requestPath: String): String {
    val normalizedPath = normalizePath(requestPath)
    if (normalizedPath == "/") return "/"
    val lastSlashIndex = normalizedPath.lastIndexOf('/')
    return if (lastSlashIndex <= 0) "/" else normalizedPath.substring(0, lastSlashIndex)
}

private fun computeExpirationMillis(cookie: Cookie, nowMs: Long): Long? {
    val maxAgeSeconds = cookie.maxAge
    if (maxAgeSeconds != null) {
        if (maxAgeSeconds <= 0) return nowMs - 1L
        return nowMs + (maxAgeSeconds.toLong() * 1_000L)
    }

    val expiresTimestamp = cookie.expires?.timestamp
    if (expiresTimestamp != null) {
        return expiresTimestamp
    }

    return nowMs + SESSION_COOKIE_FALLBACK_TTL_MS
}

private const val DEFAULT_STORAGE_KEY = "network.cookies.v1"
private const val DEFAULT_MAX_COOKIE_COUNT = 512
private const val SESSION_COOKIE_FALLBACK_TTL_MS = 7L * 24L * 60L * 60L * 1_000L
