package com.mockpack.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mockpack.ui.theme.MockPackColors

/**
 * 메타데이터 입력 폼의 공통 필드 Composable.
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MockPackColors.TextSecondary
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = MockPackColors.TextSecondary.copy(alpha = 0.5f)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )
    }
}
