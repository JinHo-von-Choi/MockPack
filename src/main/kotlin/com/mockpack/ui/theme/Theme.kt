package com.mockpack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * MockPack Material 3 테마.
 * MockPackColors 팔레트를 Material 3 ColorScheme에 매핑한다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
private val MockPackColorScheme = lightColorScheme(
    primary            = MockPackColors.Primary,
    onPrimary          = MockPackColors.OnPrimary,
    primaryContainer   = MockPackColors.PrimaryContainer,
    secondary          = MockPackColors.Secondary,
    onSecondary        = MockPackColors.OnSecondary,
    background         = MockPackColors.Background,
    onBackground       = MockPackColors.OnBackground,
    surface            = MockPackColors.Surface,
    onSurface          = MockPackColors.OnSurface,
    surfaceVariant     = MockPackColors.SurfaceVariant,
    error              = MockPackColors.Error,
    onError            = MockPackColors.OnError,
    outline            = MockPackColors.Border
)

@Composable
fun MockPackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MockPackColorScheme,
        typography  = MockPackTypography,
        content     = content
    )
}
