package com.wahon.app.ui.screen.updates

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.app.navigation.BrowseOpenRequest
import com.wahon.app.navigation.BrowseOpenOrigin
import com.wahon.app.navigation.BrowseOpenRequestBus
import com.wahon.shared.domain.model.UpdateEntry
import com.wahon.shared.domain.repository.UpdatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdatesScreenModel(
    private val updatesRepository: UpdatesRepository,
    private val browseOpenRequestBus: BrowseOpenRequestBus,
) : ScreenModel {

    private val _state = MutableStateFlow(UpdatesUiState(isLoading = true))
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            updatesRepository.observeRecentLibraryUpdates().collectLatest { updates ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        updates = updates,
                        error = null,
                    )
                }
            }
        }
    }

    fun openManga(entry: UpdateEntry) {
        browseOpenRequestBus.emit(
            BrowseOpenRequest(
                sourceId = entry.sourceId,
                mangaUrl = entry.mangaUrl,
                origin = BrowseOpenOrigin.UPDATES,
            ),
        )
    }

    fun resumeChapter(entry: UpdateEntry) {
        browseOpenRequestBus.emit(
            BrowseOpenRequest(
                sourceId = entry.sourceId,
                mangaUrl = entry.mangaUrl,
                chapterUrl = entry.chapterUrl,
                resumePage = entry.lastPageRead,
                origin = BrowseOpenOrigin.UPDATES,
            ),
        )
    }
}

data class UpdatesUiState(
    val isLoading: Boolean = false,
    val updates: List<UpdateEntry> = emptyList(),
    val error: String? = null,
)
