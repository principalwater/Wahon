package com.wahon.shared.data.remote.dto

import com.wahon.shared.domain.model.ExtensionInfo
import com.wahon.shared.domain.model.ExtensionRepo

fun SourceListDto.toExtensionRepo(url: String): ExtensionRepo {
    return ExtensionRepo(
        url = url,
        name = name,
    )
}

fun SourceEntryDto.toExtensionInfo(repoBaseUrl: String): ExtensionInfo {
    val base = repoBaseUrl.trimEnd('/')
    val resolvedIcon = firstNotBlank(iconUrl, legacyIcon)
    val resolvedDownload = firstNotBlank(downloadUrl, legacyFile)
    val resolvedBaseUrl = firstNotBlank(baseUrl, legacyBaseUrl)
    val resolvedLanguages = when {
        languages.isNotEmpty() -> languages
        legacyLanguage.isNotBlank() -> listOf(legacyLanguage)
        else -> emptyList()
    }
    val resolvedContentRating = nsfw ?: contentRating

    return ExtensionInfo(
        id = id,
        name = name,
        version = version,
        iconUrl = resolvedIcon.toAbsoluteUrl(base),
        downloadUrl = resolvedDownload.toAbsoluteUrl(base),
        languages = resolvedLanguages,
        nsfw = resolvedContentRating >= 2,
        baseUrl = resolvedBaseUrl,
        repoUrl = repoBaseUrl,
    )
}

private fun firstNotBlank(primary: String, fallback: String): String {
    return primary.takeIf { it.isNotBlank() } ?: fallback
}

private fun String.toAbsoluteUrl(repoBaseUrl: String): String {
    if (isBlank()) return ""
    return if (startsWith("http://") || startsWith("https://")) this else "$repoBaseUrl/$this"
}
