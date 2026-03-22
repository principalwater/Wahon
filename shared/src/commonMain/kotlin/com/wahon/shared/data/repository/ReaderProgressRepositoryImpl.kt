package com.wahon.shared.data.repository

import com.wahon.shared.data.local.WahonDatabase
import com.wahon.shared.domain.model.ChapterProgress
import com.wahon.shared.domain.model.MangaLastRead
import com.wahon.shared.domain.repository.ReaderProgressRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReaderProgressRepositoryImpl(
    private val database: WahonDatabase,
) : ReaderProgressRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getChapterProgress(
        sourceId: String,
        chapterUrl: String,
    ): ChapterProgress? {
        val raw = database.source_dataQueries
            .selectSourceDataValue(
                source_id = sourceId,
                key = chapterProgressKey(chapterUrl),
            )
            .executeAsOneOrNull()
            ?: return null

        val payload = runCatching {
            json.decodeFromString(ChapterProgressPayload.serializer(), raw)
        }.getOrNull() ?: return null

        return payload.toDomain(chapterUrl = chapterUrl)
    }

    override suspend fun getChapterProgressMap(
        sourceId: String,
        chapterUrls: List<String>,
    ): Map<String, ChapterProgress> {
        if (chapterUrls.isEmpty()) return emptyMap()

        return buildMap {
            chapterUrls.forEach { chapterUrl ->
                val progress = getChapterProgress(
                    sourceId = sourceId,
                    chapterUrl = chapterUrl,
                )
                if (progress != null) {
                    put(chapterUrl, progress)
                }
            }
        }
    }

    override suspend fun saveChapterProgress(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
        chapterName: String,
        lastPageRead: Int,
        totalPages: Int,
        completed: Boolean,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()

        val chapterPayload = ChapterProgressPayload(
            chapterName = chapterName,
            lastPageRead = lastPageRead.coerceAtLeast(0),
            totalPages = totalPages.coerceAtLeast(0),
            completed = completed,
            lastReadAt = now,
        )
        database.source_dataQueries.upsertSourceData(
            sourceId,
            chapterProgressKey(chapterUrl),
            json.encodeToString(chapterPayload),
        )

        val mangaPayload = MangaLastReadPayload(
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            lastPageRead = lastPageRead.coerceAtLeast(0),
            totalPages = totalPages.coerceAtLeast(0),
            completed = completed,
            lastReadAt = now,
        )
        database.source_dataQueries.upsertSourceData(
            sourceId,
            mangaLastReadKey(mangaUrl),
            json.encodeToString(mangaPayload),
        )
    }

    override suspend fun getMangaLastRead(
        sourceId: String,
        mangaUrl: String,
    ): MangaLastRead? {
        val raw = database.source_dataQueries
            .selectSourceDataValue(
                source_id = sourceId,
                key = mangaLastReadKey(mangaUrl),
            )
            .executeAsOneOrNull()
            ?: return null

        val payload = runCatching {
            json.decodeFromString(MangaLastReadPayload.serializer(), raw)
        }.getOrNull() ?: return null

        return payload.toDomain(mangaUrl = mangaUrl)
    }

    private fun chapterProgressKey(chapterUrl: String): String {
        return "chapter_progress::$chapterUrl"
    }

    private fun mangaLastReadKey(mangaUrl: String): String {
        return "manga_last_read::$mangaUrl"
    }
}

@Serializable
private data class ChapterProgressPayload(
    val chapterName: String,
    val lastPageRead: Int,
    val totalPages: Int,
    val completed: Boolean,
    val lastReadAt: Long,
)

@Serializable
private data class MangaLastReadPayload(
    val chapterUrl: String,
    val chapterName: String,
    val lastPageRead: Int,
    val totalPages: Int,
    val completed: Boolean,
    val lastReadAt: Long,
)

private fun ChapterProgressPayload.toDomain(chapterUrl: String): ChapterProgress {
    return ChapterProgress(
        chapterUrl = chapterUrl,
        chapterName = chapterName,
        lastPageRead = lastPageRead,
        totalPages = totalPages,
        completed = completed,
        lastReadAt = lastReadAt,
    )
}

private fun MangaLastReadPayload.toDomain(mangaUrl: String): MangaLastRead {
    return MangaLastRead(
        mangaUrl = mangaUrl,
        chapterUrl = chapterUrl,
        chapterName = chapterName,
        lastPageRead = lastPageRead,
        totalPages = totalPages,
        completed = completed,
        lastReadAt = lastReadAt,
    )
}
