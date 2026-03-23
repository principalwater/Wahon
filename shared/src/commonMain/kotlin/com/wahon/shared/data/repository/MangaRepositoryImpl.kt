package com.wahon.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wahon.shared.data.local.WahonDatabase
import com.wahon.shared.data.remote.currentTimeMillis
import com.wahon.shared.domain.model.Chapter
import com.wahon.shared.domain.model.Manga
import com.wahon.shared.domain.model.MangaStatus
import com.wahon.shared.domain.repository.MangaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MangaRepositoryImpl(
    private val database: WahonDatabase,
) : MangaRepository {

    override fun getLibraryManga(): Flow<List<Manga>> {
        return database.mangaQueries
            .selectLibraryManga()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { row -> row.toDomainModel() } }
    }

    override suspend fun getMangaById(id: String): Manga? {
        return database.mangaQueries
            .selectMangaById(id)
            .executeAsOneOrNull()
            ?.toDomainModel()
    }

    override suspend fun addToLibrary(manga: Manga) {
        val now = currentTimeMillis()
        val createdAt = database.mangaQueries
            .selectMangaById(manga.id) { _, _, _, _, _, _, _, _, _, _, created_at, _ ->
                created_at
            }
            .executeAsOneOrNull() ?: now

        database.mangaQueries.upsertManga(
            id = manga.id,
            source_id = manga.sourceId,
            url = manga.url,
            title = manga.title,
            author = manga.author.ifBlank { null },
            artist = manga.artist.ifBlank { null },
            description = manga.description.ifBlank { null },
            cover_url = manga.coverUrl.ifBlank { null },
            status = manga.status.toDatabaseValue(),
            in_library = 1L,
            created_at = createdAt,
            updated_at = now,
        )
    }

    override suspend fun removeFromLibrary(mangaId: String) {
        database.mangaQueries.updateMangaLibraryFlag(
            in_library = 0L,
            updated_at = currentTimeMillis(),
            id = mangaId,
        )
    }

    override suspend fun upsertMangaWithChapters(manga: Manga, chapters: List<Chapter>) {
        val now = currentTimeMillis()
        val existingManga = database.mangaQueries.selectMangaById(manga.id).executeAsOneOrNull()
        val inLibraryFlag = if (manga.inLibrary || existingManga?.in_library == 1L) 1L else 0L
        val createdAt = existingManga?.created_at ?: now
        val updatedAt = if (inLibraryFlag == 1L && existingManga != null) {
            existingManga.updated_at
        } else {
            now
        }

        database.mangaQueries.upsertManga(
            id = manga.id,
            source_id = manga.sourceId,
            url = manga.url,
            title = manga.title,
            author = manga.author.ifBlank { null },
            artist = manga.artist.ifBlank { null },
            description = manga.description.ifBlank { null },
            cover_url = manga.coverUrl.ifBlank { null },
            status = manga.status.toDatabaseValue(),
            in_library = inLibraryFlag,
            created_at = createdAt,
            updated_at = updatedAt,
        )

        chapters.forEach { chapter ->
            val existingChapter = database.chapterQueries.selectChapterById(chapter.id).executeAsOneOrNull()
            val mergedRead = (existingChapter?.read == 1L) || chapter.read
            val mergedLastPageRead = maxOf(existingChapter?.last_page_read ?: 0L, chapter.lastPageRead.toLong())

            database.chapterQueries.upsertChapter(
                id = chapter.id,
                manga_id = manga.id,
                url = chapter.url,
                name = chapter.name,
                chapter_number = chapter.chapterNumber.takeIf { it >= 0f }?.toDouble(),
                date_upload = chapter.dateUpload.takeIf { it > 0L },
                read = if (mergedRead) 1L else 0L,
                last_page_read = mergedLastPageRead,
                created_at = existingChapter?.created_at ?: now,
                updated_at = now,
            )
        }
    }

    override suspend fun getChapters(mangaId: String): List<Chapter> {
        return database.chapterQueries
            .selectChaptersByMangaId(mangaId)
            .executeAsList()
            .map { row -> row.toDomainModel() }
    }

    override suspend fun updateChapterProgress(
        chapterId: String,
        lastPageRead: Int,
        read: Boolean,
        mergeWithExisting: Boolean,
        trackHistory: Boolean,
    ) {
        val existingChapter = database.chapterQueries.selectChapterById(chapterId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()
        val targetLastPageRead = if (mergeWithExisting) {
            maxOf(existingChapter.last_page_read, lastPageRead.toLong())
        } else {
            lastPageRead.toLong().coerceAtLeast(0L)
        }
        val targetRead = if (mergeWithExisting) {
            read || existingChapter.read == 1L
        } else {
            read
        }

        database.chapterQueries.updateChapterProgress(
            last_page_read = targetLastPageRead,
            read = if (targetRead) 1L else 0L,
            updated_at = now,
            id = chapterId,
        )
        if (trackHistory) {
            database.historyQueries.upsertHistory(
                chapter_id = chapterId,
                last_read_at = now,
                time_read_seconds = 0L,
            )
            database.historyQueries.pruneHistoryKeepLatest(keepCount = MAX_HISTORY_ROWS)
        }
    }
}

private fun com.wahon.shared.data.local.Manga.toDomainModel(): Manga {
    return Manga(
        id = id,
        title = title,
        artist = artist.orEmpty(),
        author = author.orEmpty(),
        description = description.orEmpty(),
        coverUrl = cover_url.orEmpty(),
        status = status.toMangaStatus(),
        genres = emptyList(),
        inLibrary = in_library != 0L,
        sourceId = source_id,
        url = url,
    )
}

private fun com.wahon.shared.data.local.Chapter.toDomainModel(): Chapter {
    return Chapter(
        id = id,
        mangaId = manga_id,
        name = name,
        chapterNumber = chapter_number?.toFloat() ?: -1f,
        dateUpload = date_upload ?: 0L,
        read = read != 0L,
        lastPageRead = last_page_read.toInt(),
        url = url,
        scanlator = "",
    )
}

private fun MangaStatus.toDatabaseValue(): String {
    return name
}

private fun String?.toMangaStatus(): MangaStatus {
    val normalized = this?.trim()?.uppercase().orEmpty()
    if (normalized.isBlank()) return MangaStatus.UNKNOWN
    return runCatching {
        MangaStatus.valueOf(normalized)
    }.getOrDefault(MangaStatus.UNKNOWN)
}

private const val MAX_HISTORY_ROWS = 2_000L
