package com.wahon.app.ui.screen.browse

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.extension.MangaInfo
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
                val sortedSources = loadedSources.sortedBy { source -> source.name.lowercase() }
                _state.update { current ->
                    val selectedStillExists = current.selectedSourceId?.let { selectedId ->
                        sortedSources.any { source -> source.extensionId == selectedId }
                    } == true
                    if (selectedStillExists) {
                        current.copy(sources = sortedSources)
                    } else {
                        current.copy(
                            sources = sortedSources,
                            selectedSourceId = null,
                            popularManga = emptyList(),
                            isLoadingPopular = false,
                            popularPage = 0,
                            hasNextPopularPage = false,
                            popularError = null,
                        )
                    }
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

    fun openSource(extensionId: String) {
        val source = _state.value.sources.firstOrNull { it.extensionId == extensionId } ?: return
        _state.update {
            it.copy(
                selectedSourceId = extensionId,
                popularManga = emptyList(),
                isLoadingPopular = false,
                popularPage = 0,
                hasNextPopularPage = false,
                popularError = if (source.isRuntimeExecutable) null else source.runtimeMessage,
            )
        }
        if (source.isRuntimeExecutable) {
            loadPopularPage(page = 1, append = false)
        }
    }

    fun backToSourceList() {
        _state.update {
            it.copy(
                selectedSourceId = null,
                popularManga = emptyList(),
                isLoadingPopular = false,
                popularPage = 0,
                hasNextPopularPage = false,
                popularError = null,
            )
        }
    }

    fun retryCurrentSource() {
        val current = _state.value
        val selectedSource = current.selectedSource ?: return
        if (!selectedSource.isRuntimeExecutable) return
        loadPopularPage(page = 1, append = false)
    }

    fun loadNextPopularPage() {
        val current = _state.value
        val selectedSource = current.selectedSource ?: return
        if (!selectedSource.isRuntimeExecutable || current.isLoadingPopular || !current.hasNextPopularPage) {
            return
        }
        loadPopularPage(page = current.popularPage + 1, append = true)
    }

    private fun loadPopularPage(
        page: Int,
        append: Boolean,
    ) {
        val sourceId = _state.value.selectedSourceId ?: return
        if (_state.value.isLoadingPopular) return

        _state.update { current ->
            if (!append) {
                current.copy(
                    isLoadingPopular = true,
                    popularManga = emptyList(),
                    popularError = null,
                    popularPage = 0,
                    hasNextPopularPage = false,
                )
            } else {
                current.copy(
                    isLoadingPopular = true,
                    popularError = null,
                )
            }
        }

        screenModelScope.launch {
            extensionRuntimeRepository.getPopularManga(sourceId, page)
                .onSuccess { mangaPage ->
                    _state.update { current ->
                        if (current.selectedSourceId != sourceId) return@update current
                        val merged = if (append) {
                            (current.popularManga + mangaPage.manga).distinctBy { it.url }
                        } else {
                            mangaPage.manga
                        }
                        current.copy(
                            isLoadingPopular = false,
                            popularManga = merged,
                            popularPage = page,
                            hasNextPopularPage = mangaPage.hasNextPage,
                            popularError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        if (current.selectedSourceId != sourceId) return@update current
                        current.copy(
                            isLoadingPopular = false,
                            popularError = error.message ?: "Failed to load popular manga",
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
    val selectedSourceId: String? = null,
    val popularManga: List<MangaInfo> = emptyList(),
    val isLoadingPopular: Boolean = false,
    val popularPage: Int = 0,
    val hasNextPopularPage: Boolean = false,
    val popularError: String? = null,
) {
    val selectedSource: LoadedSource?
        get() = selectedSourceId?.let { selectedId ->
            sources.firstOrNull { source -> source.extensionId == selectedId }
        }

    val isEmpty: Boolean
        get() = !isReloading && sources.isEmpty()
}
