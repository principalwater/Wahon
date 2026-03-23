package com.wahon.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wahon.shared.data.local.WahonDatabase
import com.wahon.shared.domain.model.UpdateEntry
import com.wahon.shared.domain.repository.UpdatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UpdatesRepositoryImpl(
    private val database: WahonDatabase,
) : UpdatesRepository {

    override fun observeRecentLibraryUpdates(limit: Long): Flow<List<UpdateEntry>> {
        return database.chapterQueries
            .selectRecentLibraryUpdates(value_ = limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    UpdateEntry(
                        chapterId = row.id,
                        sourceId = row.manga_source_id,
                        mangaId = row.manga_id,
                        mangaUrl = row.manga_url,
                        mangaTitle = row.manga_title,
                        mangaCoverUrl = row.manga_cover_url.orEmpty(),
                        chapterUrl = row.url,
                        chapterName = row.name,
                        chapterNumber = row.chapter_number?.toFloat() ?: -1f,
                        dateUpload = row.date_upload ?: 0L,
                        lastPageRead = row.last_page_read.toInt(),
                        read = row.read == 1L,
                        updatedAt = row.created_at,
                    )
                }
            }
    }
}
