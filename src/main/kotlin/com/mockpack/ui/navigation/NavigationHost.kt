package com.mockpack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

/**
 * MockPack 화면 식별자.
 *
 * @author 최진호
 * @since 2026-02-27
 */
enum class Screen {
    EXTRACT,
    BUILD
}

/**
 * 현재 선택된 Screen에 따라 적절한 화면을 표시한다.
 *
 * @param currentScreen 현재 활성 화면 상태
 * @param extractContent Extract 화면 Composable
 * @param buildContent Build 화면 Composable
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun NavigationHost(
    currentScreen: Screen,
    extractContent: @Composable () -> Unit,
    buildContent: @Composable () -> Unit
) {
    when (currentScreen) {
        Screen.EXTRACT -> extractContent()
        Screen.BUILD   -> buildContent()
    }
}
