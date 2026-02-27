package com.mockpack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.core.model.AppMetadata
import com.mockpack.core.model.IosMetadata
import com.mockpack.ui.theme.MockPackColors

/**
 * 추출된 메타데이터를 키-값 테이블로 표시하는 컴포넌트.
 *
 * @param metadata 표시할 메타데이터
 * @param modifier Modifier
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun MetadataTable(
    metadata: AppMetadata,
    modifier: Modifier = Modifier
) {
    val rows = buildRows(metadata)
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MockPackColors.Border, shape)
            .background(MockPackColors.Surface)
    ) {
        rows.forEachIndexed { index, (key, value) ->
            MetadataRow(key = key, value = value)
            if (index < rows.lastIndex) {
                HorizontalDivider(color = MockPackColors.Border, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun MetadataRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = key,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MockPackColors.TextSecondary,
            modifier   = Modifier.weight(0.4f)
        )
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MockPackColors.OnSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun buildRows(metadata: AppMetadata): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()

    when (metadata) {
        is AndroidMetadata -> {
            rows.add("Platform" to "Android (APK)")
            rows.add("Package Name" to metadata.packageId)
            rows.add("Version Name" to metadata.versionName)
            rows.add("Version Code" to metadata.buildNumber)
            rows.add("App Name" to metadata.appName)
            rows.add("Min SDK Version" to metadata.minSdkVersion.toString())
            rows.add("Target SDK Version" to metadata.targetSdkVersion.toString())
            if (metadata.permissions.isNotEmpty()) {
                rows.add("Permissions" to metadata.permissions.joinToString(", "))
            }
        }
        is IosMetadata -> {
            rows.add("Platform" to "iOS (IPA)")
            rows.add("Bundle ID" to metadata.packageId)
            rows.add("Version" to metadata.versionName)
            rows.add("Build Number" to metadata.buildNumber)
            rows.add("App Name" to metadata.appName)
            if (metadata.minimumOSVersion.isNotBlank()) {
                rows.add("Minimum OS Version" to metadata.minimumOSVersion)
            }
            if (metadata.supportedPlatforms.isNotEmpty()) {
                rows.add("Supported Platforms" to metadata.supportedPlatforms.joinToString(", "))
            }
        }
    }

    return rows
}
