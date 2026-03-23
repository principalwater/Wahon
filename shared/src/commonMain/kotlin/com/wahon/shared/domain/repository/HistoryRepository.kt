package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeRecentHistory(limit: Long = DEFAULT_HISTORY_LIMIT): Flow<List<HistoryEntry>>
    suspend fun clearHistory()

    companion object {
        const val DEFAULT_HISTORY_LIMIT: Long = 200L
    }
}
