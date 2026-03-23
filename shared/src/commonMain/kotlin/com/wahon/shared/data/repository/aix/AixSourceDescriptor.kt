package com.wahon.shared.data.repository.aix

data class AixSourceDescriptor(
    val extensionId: String,
    val declaredSourceId: String? = null,
    val sourceName: String? = null,
    val baseUrl: String? = null,
    val language: String? = null,
) {
    fun matchesId(candidateIds: Set<String>): Boolean {
        if (candidateIds.isEmpty()) return false
        val normalizedCandidates = candidateIds.mapTo(mutableSetOf()) { it.trim().lowercase() }
        return normalizedIds.any(normalizedCandidates::contains)
    }

    fun matchesHost(candidateHosts: Set<String>): Boolean {
        if (candidateHosts.isEmpty()) return false
        val normalizedCandidates = candidateHosts.mapTo(mutableSetOf()) {
            normalizeHost(it)
        }
        return normalizedHosts.any { host ->
            normalizedCandidates.any { candidate ->
                host == candidate || host.endsWith(".$candidate")
            }
        }
    }

    private val normalizedIds: Set<String> = buildSet {
        add(extensionId.trim().lowercase())
        declaredSourceId
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?.lowercase()
            ?.let(::add)
    }

    private val normalizedHosts: Set<String> = buildSet {
        extractHost(baseUrl)?.let(::add)
    }

    companion object {
        private val SCHEME_REGEX = Regex("^https?://", RegexOption.IGNORE_CASE)

        private fun extractHost(rawUrl: String?): String? {
            val normalized = rawUrl
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.replace(SCHEME_REGEX, "")
                ?.substringBefore('/')
                ?.substringBefore('?')
                ?.substringBefore('#')
                ?.substringBefore(':')
                ?.lowercase()
                ?.trim()
                .orEmpty()
            return normalized.takeIf { it.isNotBlank() }
        }

        private fun normalizeHost(rawHost: String): String {
            return extractHost(rawHost) ?: rawHost.trim().lowercase()
        }
    }
}
