package com.wahon.extension

/**
 * Abstract contract for online catalog sources.
 * Declares method signatures for fetching popular titles,
 * searching with filters, and extracting chapter lists.
 */
interface HttpSource : Source {
    val baseUrl: String

    suspend fun getPopularManga(page: Int): MangaPage
    suspend fun searchManga(query: String, page: Int, filters: List<Filter>): MangaPage
    suspend fun getMangaDetails(mangaUrl: String): MangaInfo
    suspend fun getChapterList(mangaUrl: String): List<ChapterInfo>
    suspend fun getPageList(chapterUrl: String): List<PageInfo>
}
