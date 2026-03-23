package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.UpdateEntry
import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {
    fun observeRecentLibraryUpdates(limit: Long = DEFAULT_UPDATES_LIMIT): Flow<List<UpdateEntry>>

    companion object {
        const val DEFAULT_UPDATES_LIMIT: Long = 300L
    }
}
