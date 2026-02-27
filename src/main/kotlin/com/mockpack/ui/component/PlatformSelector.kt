package com.mockpack.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mockpack.ui.theme.MockPackColors

/**
 * Android / iOS 플랫폼 선택 토글 버튼.
 *
 * @param selectedPlatform 현재 선택된 플랫폼 ("android" 또는 "ios")
 * @param onSelect 플랫폼 선택 콜백
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun PlatformSelector(
    selectedPlatform: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        PlatformChip(
            label      = "Android (APK)",
            isSelected = selectedPlatform == "android",
            onClick    = { onSelect("android") },
            shape      = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
        )
        PlatformChip(
            label      = "iOS (IPA)",
            isSelected = selectedPlatform == "ios",
            onClick    = { onSelect("ios") },
            shape      = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        )
    }
}

@Composable
private fun PlatformChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val bgColor     = if (isSelected) MockPackColors.Primary else MockPackColors.Surface
    val textColor   = if (isSelected) MockPackColors.OnPrimary else MockPackColors.OnSurface
    val borderColor = if (isSelected) MockPackColors.Primary else MockPackColors.Border

    Surface(
        onClick     = onClick,
        shape       = shape,
        color       = bgColor,
        border      = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelLarge,
            color    = textColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
