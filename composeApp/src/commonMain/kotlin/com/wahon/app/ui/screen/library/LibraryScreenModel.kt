package com.wahon.app.ui.screen.library

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.app.navigation.BrowseOpenRequest
import com.wahon.app.navigation.BrowseOpenOrigin
import com.wahon.app.navigation.BrowseOpenRequestBus
import com.wahon.shared.domain.model.Manga
import com.wahon.shared.domain.model.MangaLastRead
import com.wahon.shared.domain.model.LOCAL_CBZ_SOURCE_ID
import com.wahon.shared.domain.repository.ExtensionRuntimeRepository
import com.wahon.shared.domain.repository.LocalArchiveRepository
import com.wahon.shared.domain.repository.MangaRepository
import com.wahon.shared.domain.repository.ReaderProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryScreenModel(
    private val mangaRepository: MangaRepository,
    private val readerProgressRepository: ReaderProgressRepository,
    private val extensionRuntimeRepository: ExtensionRuntimeRepository,
    private val localArchiveRepository: LocalArchiveRepository,
    private val browseOpenRequestBus: BrowseOpenRequestBus,
) : ScreenModel {

    private val _state = MutableStateFlow(LibraryUiState(isLoading = true))
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            mangaRepository.getLibraryManga().collectLatest { manga ->
                val resumeState = loadResumeState(manga)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        manga = manga,
                        resumeByMangaId = resumeState,
                        error = null,
                    )
                }
            }
        }

        screenModelScope.launch {
            extensionRuntimeRepository.loadedSources.collectLatest { loadedSources ->
                val sourceNameById = loadedSources.associate { source ->
                    source.extensionId to source.name
                }
                _state.update { current ->
                    current.copy(sourceNameById = sourceNameById)
                }
            }
        }

        screenModelScope.launch {
            extensionRuntimeRepository.reloadInstalledSources()
        }
    }

    fun removeFromLibrary(manga: Manga) {
        screenModelScope.launch {
            runCatching {
                if (manga.sourceId == LOCAL_CBZ_SOURCE_ID) {
                    localArchiveRepository.removeImportedCbz(mangaUrl = manga.url)
                        .getOrElse { error ->
                            throw error
                        }
                } else {
                    mangaRepository.removeFromLibrary(manga.id)
                }
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        error = error.message ?: "Failed to remove manga from library",
                    )
                }
            }
        }
    }

    fun openManga(manga: Manga) {
        val sourceId = manga.sourceId.trim()
        val mangaUrl = manga.url.trim()
        if (sourceId.isBlank() || mangaUrl.isBlank()) {
            _state.update { current ->
                current.copy(error = "Cannot open manga: missing source metadata")
            }
            return
        }

        browseOpenRequestBus.emit(
            BrowseOpenRequest(
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                origin = BrowseOpenOrigin.LIBRARY,
            ),
        )
    }

    fun resumeManga(manga: Manga) {
        val sourceId = manga.sourceId.trim()
        val mangaUrl = manga.url.trim()
        if (sourceId.isBlank() || mangaUrl.isBlank()) {
            _state.update { current ->
                current.copy(error = "Cannot resume manga: missing source metadata")
            }
            return
        }

        screenModelScope.launch {
            val lastRead = runCatching {
                readerProgressRepository.getMangaLastRead(
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                )
            }.getOrNull()

            browseOpenRequestBus.emit(
                BrowseOpenRequest(
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                    chapterUrl = lastRead?.chapterUrl,
                    resumePage = lastRead?.lastPageRead,
                    origin = BrowseOpenOrigin.LIBRARY,
                ),
            )
        }
    }

    private suspend fun loadResumeState(mangaItems: List<Manga>): Map<String, MangaLastRead> {
        if (mangaItems.isEmpty()) return emptyMap()

        val resumeMap = mutableMapOf<String, MangaLastRead>()
        mangaItems.forEach { manga ->
            val sourceId = manga.sourceId.trim()
            val mangaUrl = manga.url.trim()
            if (sourceId.isBlank() || mangaUrl.isBlank()) {
                return@forEach
            }

            val lastRead = runCatching {
                readerProgressRepository.getMangaLastRead(
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                )
            }.getOrNull()

            if (lastRead != null) {
                resumeMap[manga.id] = lastRead
            }
        }
        return resumeMap
    }
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val manga: List<Manga> = emptyList(),
    val resumeByMangaId: Map<String, MangaLastRead> = emptyMap(),
    val sourceNameById: Map<String, String> = emptyMap(),
    val error: String? = null,
)
