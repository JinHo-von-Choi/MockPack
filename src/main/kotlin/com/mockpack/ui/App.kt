package com.mockpack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mockpack.ui.navigation.NavigationHost
import com.mockpack.ui.navigation.Screen
import com.mockpack.ui.screen.BuildScreen
import com.mockpack.ui.screen.ExtractScreen
import com.mockpack.ui.theme.MockPackColors
import com.mockpack.ui.theme.MockPackTheme
import com.mockpack.ui.viewmodel.BuildViewModel
import com.mockpack.ui.viewmodel.ExtractViewModel

/**
 * MockPack 루트 Composable.
 * 사이드바(네비게이션) + 콘텐츠 영역 레이아웃.
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun App(window: java.awt.Window) {
    val extractViewModel = remember { ExtractViewModel() }
    val buildViewModel   = remember { BuildViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.EXTRACT) }

    MockPackTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MockPackColors.Background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    currentScreen  = currentScreen,
                    onScreenSelect = { currentScreen = it }
                )

                VerticalDivider(color = MockPackColors.Border, thickness = 1.dp)

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavigationHost(
                        currentScreen  = currentScreen,
                        extractContent = { ExtractScreen(viewModel = extractViewModel, window = window) },
                        buildContent   = { BuildScreen(buildViewModel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    currentScreen: Screen,
    onScreenSelect: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(MockPackColors.Surface)
            .padding(12.dp)
    ) {
        Text(
            text       = "MockPack",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MockPackColors.OnSurface,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SidebarItem(
            label      = "Extract",
            isSelected = currentScreen == Screen.EXTRACT,
            onClick    = { onScreenSelect(Screen.EXTRACT) }
        )

        SidebarItem(
            label      = "Build",
            isSelected = currentScreen == Screen.BUILD,
            onClick    = { onScreenSelect(Screen.BUILD) }
        )
    }
}

@Composable
private fun SidebarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor   = if (isSelected) MockPackColors.PrimaryContainer else MockPackColors.Surface
    val textColor = if (isSelected) MockPackColors.Primary else MockPackColors.TextSecondary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodyLarge,
            color      = textColor,
            fontWeight = fontWeight
        )
    }
}
