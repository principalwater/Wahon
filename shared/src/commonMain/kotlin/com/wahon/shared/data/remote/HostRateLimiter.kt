package com.wahon.shared.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HostThrottleProfile(
    val hostPattern: String,
    val minIntervalMs: Long,
)

class HostRateLimiter(
    private val defaultMinIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    profiles: List<HostThrottleProfile> = defaultHostThrottleProfiles(),
) {
    private val mutex = Mutex()
    private val nextAllowedByHost = mutableMapOf<String, Long>()
    private val compiledProfiles: List<CompiledHostProfile> = profiles
        .mapNotNull { profile ->
            val pattern = normalizePattern(profile.hostPattern) ?: return@mapNotNull null
            CompiledHostProfile(
                pattern = pattern,
                minIntervalMs = profile.minIntervalMs.coerceAtLeast(0L),
                wildcard = pattern.startsWith(WILDCARD_PREFIX),
            )
        }
        .sortedWith(
            compareByDescending<CompiledHostProfile> { it.pattern.length }
                .thenBy { it.wildcard },
        )

    suspend fun acquire(host: String) {
        val normalizedHost = host.lowercase().trim()
        if (normalizedHost.isBlank()) return
        var waitMs = 0L

        mutex.withLock {
            val now = currentTimeMillis()
            val minIntervalMs = resolveMinIntervalMs(normalizedHost)
            val nextAllowed = nextAllowedByHost[normalizedHost] ?: 0L
            if (now < nextAllowed) {
                waitMs = nextAllowed - now
            }
            val scheduledAt = maxOf(now, nextAllowed) + minIntervalMs
            nextAllowedByHost[normalizedHost] = scheduledAt
        }

        if (waitMs > 0L) {
            delay(waitMs)
        }
    }

    private fun resolveMinIntervalMs(host: String): Long {
        val matchedProfile = compiledProfiles.firstOrNull { profile ->
            profile.matches(host)
        }
        return matchedProfile?.minIntervalMs ?: defaultMinIntervalMs
    }
}

private data class CompiledHostProfile(
    val pattern: String,
    val minIntervalMs: Long,
    val wildcard: Boolean,
) {
    fun matches(host: String): Boolean {
        if (!wildcard) return host == pattern
        val domain = pattern.removePrefix(WILDCARD_PREFIX)
        return host == domain || host.endsWith(".$domain")
    }
}

private fun normalizePattern(rawPattern: String): String? {
    val pattern = rawPattern.lowercase().trim()
    if (pattern.isBlank()) return null
    if (pattern == WILDCARD_PREFIX) return null
    if (!pattern.startsWith(WILDCARD_PREFIX)) return pattern
    val suffix = pattern.removePrefix(WILDCARD_PREFIX)
    return if (suffix.isBlank()) null else "$WILDCARD_PREFIX$suffix"
}

fun defaultHostThrottleProfiles(): List<HostThrottleProfile> {
    return listOf(
        HostThrottleProfile(hostPattern = "raw.githubusercontent.com", minIntervalMs = 350L),
        HostThrottleProfile(hostPattern = "api.github.com", minIntervalMs = 350L),
        HostThrottleProfile(hostPattern = "*.githubusercontent.com", minIntervalMs = 350L),
    )
}

private const val DEFAULT_MIN_INTERVAL_MS = 500L
private const val WILDCARD_PREFIX = "*."
