package org.example.project

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        state = rememberWindowState(size = DpSize(800.dp, 400.dp)),
        onCloseRequest = ::exitApplication,
        title = "aabtoapk"
    ) {
        App()
    }
}