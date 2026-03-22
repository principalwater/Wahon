package com.wahon.app

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.wahon.app.ui.screen.home.HomeScreen
import com.wahon.app.ui.theme.WahonTheme

@Composable
fun App() {
    WahonTheme {
        Navigator(HomeScreen())
    }
}
