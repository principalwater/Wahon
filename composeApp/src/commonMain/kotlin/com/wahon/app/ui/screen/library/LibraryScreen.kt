package com.wahon.app.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wahon.shared.domain.model.Manga
import com.wahon.shared.domain.model.MangaLastRead
import com.wahon.shared.domain.model.MangaStatus

@Composable
fun LibraryScreen(
    screenModel: LibraryScreenModel,
    modifier: Modifier = Modifier,
    onNavigateToBrowse: () -> Unit = {},
) {
    val state by screenModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.manga.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Library is empty",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Open a source and tap Add to Library in manga details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "library_header") {
                    Text(
                        text = "Library: ${state.manga.size}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    val error = state.error
                    if (!error.isNullOrBlank()) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                items(
                    items = state.manga,
                    key = { manga -> manga.id },
                ) { manga ->
                    LibraryMangaItem(
                        manga = manga,
                        sourceName = state.sourceNameById[manga.sourceId].orEmpty(),
                        resumeInfo = state.resumeByMangaId[manga.id],
                        onOpen = {
                            screenModel.openManga(manga)
                            onNavigateToBrowse()
                        },
                        onResume = {
                            screenModel.resumeManga(manga)
                            onNavigateToBrowse()
                        },
                        onRemove = { screenModel.removeFromLibrary(manga) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryMangaItem(
    manga: Manga,
    sourceName: String,
    resumeInfo: MangaLastRead?,
    onOpen: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val hasResume = resumeInfo != null

    Surface(
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
                    .filter { value -> value.isNotBlank() }
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
                Text(
                    text = "Source: ${sourceName.ifBlank { manga.sourceId.ifBlank { "Unknown source" } }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (manga.status != MangaStatus.UNKNOWN) {
                    Text(
                        text = manga.status.name.lowercase().replaceFirstChar(Char::uppercase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (resumeInfo != null) {
                    val progressText = buildString {
                        append("Resume from ")
                        append(resumeInfo.chapterName.ifBlank { "last chapter" })
                        if (resumeInfo.totalPages > 0) {
                            append(" • ")
                            append((resumeInfo.lastPageRead + 1).coerceAtLeast(1))
                            append("/")
                            append(resumeInfo.totalPages)
                        }
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = if (hasResume) onResume else onOpen,
                    modifier = Modifier.defaultMinSize(minWidth = 96.dp),
                ) {
                    Text(if (hasResume) "Resume" else "Open")
                }
                if (hasResume) {
                    OutlinedButton(
                        onClick = onOpen,
                        modifier = Modifier.defaultMinSize(minWidth = 96.dp),
                    ) {
                        Text("Open")
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}
