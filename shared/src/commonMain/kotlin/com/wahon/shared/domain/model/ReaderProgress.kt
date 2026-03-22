package com.wahon.shared.domain.model

data class ChapterProgress(
    val chapterUrl: String,
    val chapterName: String,
    val lastPageRead: Int,
    val totalPages: Int,
    val completed: Boolean,
    val lastReadAt: Long,
)

data class MangaLastRead(
    val mangaUrl: String,
    val chapterUrl: String,
    val chapterName: String,
    val lastPageRead: Int,
    val totalPages: Int,
    val completed: Boolean,
    val lastReadAt: Long,
)
