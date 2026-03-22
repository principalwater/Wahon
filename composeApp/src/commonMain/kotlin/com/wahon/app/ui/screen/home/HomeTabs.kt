package com.wahon.app.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.wahon.app.ui.screen.browse.BrowseScreen
import com.wahon.app.ui.screen.browse.ExtensionsScreenModel
import com.wahon.app.ui.screen.browse.SourcesScreenModel
import com.wahon.app.ui.screen.more.MoreScreenWrapper

object LibraryTab : Tab {
    private fun readResolve(): Any = LibraryTab

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.CollectionsBookmark)
            return remember { TabOptions(index = 0u, title = "Library", icon = icon) }
        }

    @Composable
    override fun Content() {
        PlaceholderScreen("Library")
    }
}

object UpdatesTab : Tab {
    private fun readResolve(): Any = UpdatesTab

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.NewReleases)
            return remember { TabOptions(index = 1u, title = "Updates", icon = icon) }
        }

    @Composable
    override fun Content() {
        PlaceholderScreen("Updates")
    }
}

object HistoryTab : Tab {
    private fun readResolve(): Any = HistoryTab

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.History)
            return remember { TabOptions(index = 2u, title = "History", icon = icon) }
        }

    @Composable
    override fun Content() {
        PlaceholderScreen("History")
    }
}

object BrowseTab : Tab {
    private fun readResolve(): Any = BrowseTab

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Explore)
            return remember { TabOptions(index = 3u, title = "Browse", icon = icon) }
        }

    @Composable
    override fun Content() {
        val sourcesScreenModel = koinScreenModel<SourcesScreenModel>()
        val extensionsScreenModel = koinScreenModel<ExtensionsScreenModel>()
        BrowseScreen(
            sourcesScreenModel = sourcesScreenModel,
            extensionsScreenModel = extensionsScreenModel,
        )
    }
}

object MoreTab : Tab {
    private fun readResolve(): Any = MoreTab

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.MoreHoriz)
            return remember { TabOptions(index = 4u, title = "More", icon = icon) }
        }

    @Composable
    override fun Content() {
        Navigator(MoreScreenWrapper())
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
