package com.wahon.app.ui.screen.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wahon.app.navigation.BrowseOpenOrigin

@Composable
fun BrowseScreen(
    sourcesScreenModel: SourcesScreenModel,
    extensionsScreenModel: ExtensionsScreenModel,
    onNavigateToOrigin: (BrowseOpenOrigin) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Sources", "Extensions")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> {
                SourcesScreen(
                    screenModel = sourcesScreenModel,
                    onNavigateToOrigin = onNavigateToOrigin,
                )
            }
            1 -> {
                ExtensionsScreen(screenModel = extensionsScreenModel)
            }
        }
    }
}
