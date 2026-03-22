package com.wahon.app.ui.screen.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.wahon.shared.domain.model.LoadedSource
import com.wahon.shared.domain.model.SourceRuntimeKind

@Composable
fun SourcesScreen(
    screenModel: SourcesScreenModel,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isReloading && state.sources.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            text = "Install extensions in Browse → Extensions, then reload this tab.",
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
