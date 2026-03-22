package com.wahon.app.ui.screen.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wahon.extension.MangaInfo
import com.wahon.shared.domain.model.LoadedSource
import com.wahon.shared.domain.model.SourceRuntimeKind

@Composable
fun SourcesScreen(
    screenModel: SourcesScreenModel,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()
    val selectedSource = state.selectedSource

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isReloading && state.sources.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            selectedSource != null -> {
                SourceCatalog(
                    source = selectedSource,
                    state = state,
                    onBack = screenModel::backToSourceList,
                    onRetry = screenModel::retryCurrentSource,
                    onLoadMore = screenModel::loadNextPopularPage,
                )
            }

            state.isEmpty -> {
                EmptySourcesState(
                    error = state.error,
                    onReload = screenModel::reload,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "sources_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "Installed sources: ${state.sources.size}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val reloadError = state.error
                            if (!reloadError.isNullOrBlank()) {
                                Text(
                                    text = reloadError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    items(
                        items = state.sources,
                        key = { it.extensionId },
                    ) { source ->
                        SourceListItem(
                            source = source,
                            onOpen = { screenModel.openSource(source.extensionId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }
                    item(key = "sources_reload") {
                        OutlinedButton(
                            onClick = screenModel::reload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text("Reload sources")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceListItem(
    source: LoadedSource,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Language: ${source.language.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = runtimeTitle(source),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (source.isRuntimeExecutable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                val runtimeMessage = source.runtimeMessage
                if (!runtimeMessage.isNullOrBlank()) {
                    Text(
                        text = runtimeMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(onClick = onOpen) {
                Text(if (source.isRuntimeExecutable) "Open" else "Details")
            }
        }
    }
}

private fun runtimeTitle(source: LoadedSource): String {
    val kindLabel = when (source.runtimeKind) {
        SourceRuntimeKind.JAVASCRIPT -> "JavaScript"
        SourceRuntimeKind.AIDOKU_AIX -> "Aidoku .aix (WASM)"
        SourceRuntimeKind.UNKNOWN -> "Unknown"
    }
    val status = if (source.isRuntimeExecutable) "ready" else "not executable"
    return "Runtime: $kindLabel ($status)"
}

@Composable
private fun SourceCatalog(
    source: LoadedSource,
    state: SourcesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back to sources")
        }
        Text(
            text = source.name,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = runtimeTitle(source),
            style = MaterialTheme.typography.bodySmall,
            color = if (source.isRuntimeExecutable) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )

        if (!source.isRuntimeExecutable) {
            Text(
                text = source.runtimeMessage ?: "This source runtime is not executable yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        val popularError = state.popularError
        if (!popularError.isNullOrBlank()) {
            Text(
                text = popularError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }

        if (state.isLoadingPopular && state.popularManga.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (state.popularManga.isEmpty()) {
            Text(
                text = "No popular manga returned by this source.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onRetry) {
                Text("Reload popular")
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.popularManga,
                key = { manga -> manga.url },
            ) { manga ->
                MangaListItem(
                    manga = manga,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.isLoadingPopular) {
                item(key = "popular_loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (state.hasNextPopularPage && !state.isLoadingPopular) {
                item(key = "popular_load_more") {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Text("Load more")
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaListItem(
    manga: MangaInfo,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = manga.coverUrl.ifBlank { null },
                contentDescription = manga.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val secondary = listOf(manga.author, manga.artist)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " • ")
                if (secondary.isNotBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySourcesState(
    error: String?,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No installed sources yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Install extensions in Browse -> Extensions, then reload this tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(onClick = onReload) {
            Text("Reload sources")
        }
    }
}
