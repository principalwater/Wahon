package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.Chapter
import com.wahon.shared.domain.model.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getLibraryManga(): Flow<List<Manga>>
    suspend fun getMangaById(id: String): Manga?
    suspend fun addToLibrary(manga: Manga)
    suspend fun removeFromLibrary(mangaId: String)
    suspend fun getChapters(mangaId: String): List<Chapter>
    suspend fun updateChapterProgress(chapterId: String, lastPageRead: Int, read: Boolean)
}
