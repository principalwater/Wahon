package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.ChapterProgress
import com.wahon.shared.domain.model.MangaLastRead

interface ReaderProgressRepository {
    suspend fun getChapterProgress(
        sourceId: String,
        chapterUrl: String,
    ): ChapterProgress?

    suspend fun getChapterProgressMap(
        sourceId: String,
        chapterUrls: List<String>,
    ): Map<String, ChapterProgress>

    suspend fun saveChapterProgress(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
        chapterName: String,
        lastPageRead: Int,
        totalPages: Int,
        completed: Boolean,
        updateMangaLastRead: Boolean = true,
    )

    suspend fun clearChapterProgress(
        sourceId: String,
        chapterUrl: String,
    )

    suspend fun getMangaLastRead(
        sourceId: String,
        mangaUrl: String,
    ): MangaLastRead?

    suspend fun clearMangaLastRead(
        sourceId: String,
        mangaUrl: String,
    )
}
