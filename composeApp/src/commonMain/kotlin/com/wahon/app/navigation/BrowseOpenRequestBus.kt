package com.wahon.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BrowseOpenOrigin {
    BROWSE,
    LIBRARY,
    UPDATES,
    HISTORY,
}

data class BrowseOpenRequest(
    val sourceId: String,
    val mangaUrl: String,
    val chapterUrl: String? = null,
    val resumePage: Int? = null,
    val origin: BrowseOpenOrigin = BrowseOpenOrigin.BROWSE,
)

class BrowseOpenRequestBus {
    private val _request = MutableStateFlow<BrowseOpenRequest?>(null)
    val request: StateFlow<BrowseOpenRequest?> = _request.asStateFlow()

    fun emit(request: BrowseOpenRequest) {
        _request.value = request
    }

    fun consume(request: BrowseOpenRequest) {
        if (_request.value == request) {
            _request.value = null
        }
    }
}
