package com.wahon.shared.data.local

internal const val OFFLINE_DOWNLOADS_DIR_NAME = "downloads"

internal fun mangaDownloadPrefix(
    sourceId: String,
    mangaUrl: String,
): String {
    return buildString {
        append(sanitizeSegment(sourceId))
        append('-')
        append(stableHash64Hex(mangaUrl))
        append('-')
    }
}

internal fun chapterDownloadPrefix(
    sourceId: String,
    mangaUrl: String,
    chapterUrl: String,
): String {
    return mangaDownloadPrefix(sourceId = sourceId, mangaUrl = mangaUrl) + stableHash64Hex(chapterUrl)
}

internal fun imageExtensionFromUrl(url: String): String {
    val filePart = url.substringBefore('?').substringAfterLast('/', "")
    val candidate = filePart.substringAfterLast('.', "")
        .lowercase()
        .trim()
    return if (candidate.matches(VALID_IMAGE_EXTENSION_REGEX)) candidate else "jpg"
}

internal fun sanitizeSegment(value: String): String {
    return value
        .replace(INVALID_PATH_CHARS_REGEX, "_")
        .ifBlank { "item" }
}

internal fun stableHash64Hex(value: String): String {
    var hash = FNV_OFFSET_BASIS
    value.encodeToByteArray().forEach { byte ->
        hash = (hash xor byte.toUByte().toULong()) * FNV_PRIME
    }
    return hash.toString(16)
}

private val INVALID_PATH_CHARS_REGEX = Regex("[^a-zA-Z0-9._-]")
private val VALID_IMAGE_EXTENSION_REGEX = Regex("^[a-z0-9]{1,8}$")
private val FNV_OFFSET_BASIS = 14695981039346656037uL
private val FNV_PRIME = 1099511628211uL
