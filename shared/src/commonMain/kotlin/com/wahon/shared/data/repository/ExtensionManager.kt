package com.wahon.shared.data.repository

import com.wahon.extension.ChapterInfo
import com.wahon.extension.Filter
import com.wahon.extension.MangaInfo
import com.wahon.extension.MangaPage
import com.wahon.extension.PageInfo
import com.wahon.shared.domain.model.LoadedSource
import com.wahon.shared.domain.repository.ExtensionRuntimeRepository
import kotlinx.coroutines.flow.StateFlow

class ExtensionManager(
    private val runtimeRepository: ExtensionRuntimeRepository,
) {
    val loadedSources: StateFlow<List<LoadedSource>> = runtimeRepository.loadedSources

    suspend fun reloadInstalledExtensions(): Result<List<LoadedSource>> {
        return runtimeRepository.reloadInstalledSources()
    }

    fun getLoadedSource(extensionId: String): LoadedSource? {
        return runtimeRepository.getLoadedSource(extensionId)
    }

    suspend fun getPopularManga(extensionId: String, page: Int): Result<MangaPage> {
        return runtimeRepository.getPopularManga(extensionId, page)
    }

    suspend fun searchManga(
        extensionId: String,
        query: String,
        page: Int,
        filters: List<Filter>,
    ): Result<MangaPage> {
        return runtimeRepository.searchManga(extensionId, query, page, filters)
    }

    suspend fun getMangaDetails(extensionId: String, mangaUrl: String): Result<MangaInfo> {
        return runtimeRepository.getMangaDetails(extensionId, mangaUrl)
    }

    suspend fun getChapterList(extensionId: String, mangaUrl: String): Result<List<ChapterInfo>> {
        return runtimeRepository.getChapterList(extensionId, mangaUrl)
    }

    suspend fun getPageList(extensionId: String, chapterUrl: String): Result<List<PageInfo>> {
        return runtimeRepository.getPageList(extensionId, chapterUrl)
    }
}
