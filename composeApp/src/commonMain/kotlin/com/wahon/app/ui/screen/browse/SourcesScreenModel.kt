package com.wahon.app.ui.screen.browse

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.shared.domain.model.LoadedSource
import com.wahon.shared.domain.repository.ExtensionRuntimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourcesScreenModel(
    private val extensionRuntimeRepository: ExtensionRuntimeRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(SourcesUiState())
    val state: StateFlow<SourcesUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            extensionRuntimeRepository.loadedSources.collectLatest { loadedSources ->
                _state.update {
                    it.copy(
                        sources = loadedSources.sortedBy { source -> source.name.lowercase() },
                    )
                }
            }
        }
    }

    fun reload() {
        screenModelScope.launch {
            _state.update { it.copy(isReloading = true, error = null) }
            extensionRuntimeRepository.reloadInstalledSources()
                .onSuccess {
                    _state.update { it.copy(isReloading = false, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isReloading = false,
                            error = error.message ?: "Failed to reload installed sources",
                        )
                    }
                }
        }
    }
}

data class SourcesUiState(
    val isReloading: Boolean = false,
    val sources: List<LoadedSource> = emptyList(),
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = !isReloading && sources.isEmpty()
}
