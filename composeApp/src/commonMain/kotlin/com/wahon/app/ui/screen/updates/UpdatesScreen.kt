package com.wahon.app.ui.screen.updates

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
import com.wahon.shared.domain.model.UpdateEntry
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun UpdatesScreen(
    screenModel: UpdatesScreenModel,
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

        state.updates.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "No updates yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Run library refresh and wait for new chapters from your sources.",
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
                item(key = "updates_header") {
                    Text(
                        text = "Updates: ${state.updates.size}",
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
                    items = state.updates,
                    key = { entry -> entry.chapterId },
                ) { entry ->
                    UpdateItem(
                        entry = entry,
                        onOpen = {
                            screenModel.openManga(entry)
                            onNavigateToBrowse()
                        },
                        onResume = {
                            screenModel.resumeChapter(entry)
                            onNavigateToBrowse()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateItem(
    entry: UpdateEntry,
    onOpen: () -> Unit,
    onResume: () -> Unit,
) {
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
                model = entry.mangaCoverUrl.ifBlank { null },
                contentDescription = entry.mangaTitle,
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = entry.mangaTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.chapterName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val progressText = if (entry.read) {
                    "Completed"
                } else {
                    "Page ${entry.lastPageRead + 1}"
                }
                Text(
                    text = "$progressText • ${formatUpdatesTimestamp(entry.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpen) {
                        Text("Open manga")
                    }
                    OutlinedButton(onClick = onResume) {
                        Text("Resume")
                    }
                }
            }
        }
    }
}

private fun formatUpdatesTimestamp(timestampMs: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diffMs = (now - timestampMs).coerceAtLeast(0L)
    when {
        diffMs < 60_000L -> return "just now"
        diffMs < 3_600_000L -> return "${diffMs / 60_000L}m ago"
        diffMs < 86_400_000L -> return "${diffMs / 3_600_000L}h ago"
        diffMs < 7L * 86_400_000L -> return "${diffMs / 86_400_000L}d ago"
    }

    val dateTime = Instant.fromEpochMilliseconds(timestampMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.date)
        append(' ')
        append(dateTime.hour.toTwoDigits())
        append(':')
        append(dateTime.minute.toTwoDigits())
    }
}

private fun Int.toTwoDigits(): String {
    return if (this < 10) "0$this" else toString()
}
