package com.wahon.app.ui.screen.more

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.wahon.shared.domain.model.ExtensionRepo
import com.wahon.shared.domain.repository.ExtensionRepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExtensionRepoScreenModel(
    private val repository: ExtensionRepoRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(RepoUiState())
    val state: StateFlow<RepoUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            repository.getRepos().collect { repos ->
                _state.update { it.copy(repos = repos) }
            }
        }
    }

    fun addRepo(url: String) {
        screenModelScope.launch {
            _state.update { it.copy(isAdding = true, error = null) }
            repository.addRepo(url)
                .onSuccess {
                    _state.update { it.copy(isAdding = false, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isAdding = false,
                            error = error.message ?: "Failed to add repo",
                        )
                    }
                }
        }
    }

    fun removeRepo(url: String) {
        screenModelScope.launch {
            repository.removeRepo(url)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class RepoUiState(
    val repos: List<ExtensionRepo> = emptyList(),
    val isAdding: Boolean = false,
    val error: String? = null,
)
