package com.wahon.shared.domain.model

data class UpdateEntry(
    val chapterId: String,
    val sourceId: String,
    val mangaId: String,
    val mangaUrl: String,
    val mangaTitle: String,
    val mangaCoverUrl: String,
    val chapterUrl: String,
    val chapterName: String,
    val chapterNumber: Float,
    val dateUpload: Long,
    val lastPageRead: Int,
    val read: Boolean,
    val updatedAt: Long,
)
