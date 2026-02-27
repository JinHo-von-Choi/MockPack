package com.mockpack

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.mockpack.ui.App

/**
 * MockPack 애플리케이션 진입점.
 *
 * @author 최진호
 * @since 2026-02-27
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title          = "MockPack",
        state          = WindowState(size = DpSize(960.dp, 680.dp))
    ) {
        App(window)
    }
}
