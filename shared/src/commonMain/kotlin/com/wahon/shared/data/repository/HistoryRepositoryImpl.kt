package com.wahon.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wahon.shared.data.local.WahonDatabase
import com.wahon.shared.domain.model.HistoryEntry
import com.wahon.shared.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val database: WahonDatabase,
) : HistoryRepository {

    override fun observeRecentHistory(limit: Long): Flow<List<HistoryEntry>> {
        return database.historyQueries
            .selectRecentHistoryWithDetails(value_ = limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    HistoryEntry(
                        id = row.id,
                        mangaId = row.manga_id,
                        mangaTitle = row.manga_title,
                        mangaCoverUrl = row.manga_cover_url.orEmpty(),
                        mangaUrl = row.manga_url,
                        sourceId = row.manga_source_id,
                        chapterId = row.chapter_id,
                        chapterName = row.chapter_name,
                        chapterUrl = row.chapter_url,
                        lastPageRead = row.last_page_read.toInt(),
                        lastReadAt = row.last_read_at,
                        timeReadSeconds = row.time_read_seconds,
                    )
                }
            }
    }

    override suspend fun clearHistory() {
        database.historyQueries.clearHistory()
    }
}
