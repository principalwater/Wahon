package com.wahon.shared.domain.repository

import com.wahon.extension.ChapterInfo
import com.wahon.extension.PageInfo
import com.wahon.shared.domain.model.DownloadBatchResult

interface OfflineDownloadRepository {
    suspend fun getDownloadedChapterUrls(
        sourceId: String,
        mangaUrl: String,
    ): Set<String>

    suspend fun getDownloadedPages(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
    ): List<PageInfo>?

    suspend fun downloadChapter(
        sourceId: String,
        mangaUrl: String,
        chapter: ChapterInfo,
    ): Result<Unit>

    suspend fun downloadAllChapters(
        sourceId: String,
        mangaUrl: String,
        chapters: List<ChapterInfo>,
    ): Result<DownloadBatchResult>

    suspend fun removeDownloadedChapter(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
    ): Result<Unit>

    suspend fun removeDownloadedManga(
        sourceId: String,
        mangaUrl: String,
    ): Result<Int>

    suspend fun setAutoDownloadEnabled(
        sourceId: String,
        mangaUrl: String,
        enabled: Boolean,
    )

    suspend fun isAutoDownloadEnabled(
        sourceId: String,
        mangaUrl: String,
    ): Boolean

    suspend fun runAutoDownloadForLibrary(): Result<DownloadBatchResult>
}
