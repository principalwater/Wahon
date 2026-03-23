package com.wahon.shared.domain.model

data class HistoryEntry(
    val id: Long,
    val mangaId: String,
    val mangaTitle: String,
    val mangaCoverUrl: String,
    val mangaUrl: String,
    val sourceId: String,
    val chapterId: String,
    val chapterName: String,
    val chapterUrl: String,
    val lastPageRead: Int,
    val lastReadAt: Long,
    val timeReadSeconds: Long,
)
