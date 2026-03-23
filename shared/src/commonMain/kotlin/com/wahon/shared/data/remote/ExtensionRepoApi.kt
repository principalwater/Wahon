package com.wahon.shared.data.remote

import com.wahon.shared.data.remote.dto.SourceEntryDto
import com.wahon.shared.data.remote.dto.SourceListDto
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class ExtensionRepoApi(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Fetches a community source list from a repo URL.
     * Tries index.min.json first, then falls back to index.json.
     * Supports both object format { "sources": [...] } and plain array [...].
     */
    suspend fun fetchSourceList(repoUrl: String): SourceListDto {
        val candidates = buildIndexCandidates(repoUrl)
        var lastError: Throwable? = null

        for (candidate in candidates) {
            val rawJsonResult = runCatching {
                val response = httpClient.get(candidate)
                if (!response.status.isSuccess()) {
                    error("Failed to fetch source list from $candidate: HTTP ${response.status.value}")
                }
                response.bodyAsText()
            }
            if (rawJsonResult.isSuccess) {
                val rawJson = rawJsonResult.getOrThrow()
                val trimmed = rawJson.trimStart()
                return if (trimmed.startsWith("[")) {
                    val sources = json.decodeFromString<List<SourceEntryDto>>(rawJson)
                    SourceListDto(name = "", sources = sources)
                } else {
                    json.decodeFromString(rawJson)
                }
            }
            lastError = rawJsonResult.exceptionOrNull()
        }

        val input = repoUrl.trim()
        throw lastError ?: IllegalStateException("Failed to fetch source list from $input")
    }

    suspend fun downloadExtension(downloadUrl: String): ByteArray {
        val response = httpClient.get(downloadUrl)
        if (!response.status.isSuccess()) {
            error("Failed to download extension from $downloadUrl: HTTP ${response.status.value}")
        }
        val payload = response.body<ByteArray>()
        if (payload.isEmpty()) {
            error("Downloaded extension payload is empty: $downloadUrl")
        }
        return payload
    }

    private fun buildIndexCandidates(rawUrl: String): List<String> {
        val normalizedInput = rawUrl.trim()
        val normalizedNoSlash = normalizedInput.trimEnd('/')
        if (normalizedNoSlash.isBlank()) {
            return emptyList()
        }

        val lower = normalizedNoSlash.lowercase()
        val candidates = linkedSetOf<String>()
        when {
            lower.endsWith("/index.min.json") -> {
                val base = normalizedNoSlash.removeSuffix("/index.min.json")
                candidates += normalizedNoSlash
                candidates += "$base/index.json"
            }

            lower.endsWith("/index.json") -> {
                val base = normalizedNoSlash.removeSuffix("/index.json")
                candidates += "$base/index.min.json"
                candidates += normalizedNoSlash
            }

            lower.endsWith(".json") -> {
                candidates += normalizedNoSlash
            }

            else -> {
                candidates += "$normalizedNoSlash/index.min.json"
                candidates += "$normalizedNoSlash/index.json"
            }
        }
        return candidates.toList()
    }
}
