package com.wahon.app.ui.screen.history

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.app.navigation.BrowseOpenRequest
import com.wahon.app.navigation.BrowseOpenOrigin
import com.wahon.app.navigation.BrowseOpenRequestBus
import com.wahon.shared.domain.model.HistoryEntry
import com.wahon.shared.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryScreenModel(
    private val historyRepository: HistoryRepository,
    private val browseOpenRequestBus: BrowseOpenRequestBus,
) : ScreenModel {

    private val _state = MutableStateFlow(HistoryUiState(isLoading = true))
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            historyRepository.observeRecentHistory().collectLatest { history ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        history = history,
                        error = null,
                    )
                }
            }
        }
    }

    fun clearHistory() {
        screenModelScope.launch {
            runCatching {
                historyRepository.clearHistory()
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        error = error.message ?: "Failed to clear history",
                    )
                }
            }
        }
    }

    fun openManga(entry: HistoryEntry) {
        browseOpenRequestBus.emit(
            BrowseOpenRequest(
                sourceId = entry.sourceId,
                mangaUrl = entry.mangaUrl,
                origin = BrowseOpenOrigin.HISTORY,
            ),
        )
    }

    fun resumeChapter(entry: HistoryEntry) {
        browseOpenRequestBus.emit(
            BrowseOpenRequest(
                sourceId = entry.sourceId,
                mangaUrl = entry.mangaUrl,
                chapterUrl = entry.chapterUrl,
                resumePage = entry.lastPageRead,
                origin = BrowseOpenOrigin.HISTORY,
            ),
        )
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val error: String? = null,
)
