package com.wahon.shared.domain.model

fun buildMangaId(sourceId: String, mangaUrl: String): String {
    return "${sourceId.trim()}::${mangaUrl.trim()}"
}

fun buildChapterId(sourceId: String, mangaUrl: String, chapterUrl: String): String {
    return "${buildMangaId(sourceId = sourceId, mangaUrl = mangaUrl)}::${chapterUrl.trim()}"
}
