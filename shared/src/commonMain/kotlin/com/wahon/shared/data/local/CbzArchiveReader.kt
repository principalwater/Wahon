package com.wahon.shared.data.local

expect class CbzArchiveReader() {
    fun listPages(archivePath: String): List<CbzPageEntry>
    fun readPageBytes(
        archivePath: String,
        relativePath: String,
    ): ByteArray
}

data class CbzPageEntry(
    val relativePath: String,
    val fileName: String,
)

internal fun isSupportedCbzImage(path: String): Boolean {
    val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in SUPPORTED_IMAGE_EXTENSIONS
}

internal fun naturalSortKey(raw: String): String {
    val lower = raw.lowercase()
    return DIGIT_GROUP_REGEX.replace(lower) { match ->
        match.value.padStart(NATURAL_SORT_DIGIT_WIDTH, '0')
    }
}

private val SUPPORTED_IMAGE_EXTENSIONS = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp",
    "avif",
    "heif",
    "heic",
    "gif",
    "bmp",
)
private val DIGIT_GROUP_REGEX = Regex("\\d+")
private const val NATURAL_SORT_DIGIT_WIDTH = 12
