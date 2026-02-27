package com.mockpack.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mockpack.ui.component.FileDropZone
import com.mockpack.ui.component.MetadataTable
import com.mockpack.ui.theme.MockPackColors
import com.mockpack.ui.viewmodel.ExtractViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Extract 화면.
 * 파일 드래그앤드롭 또는 파일 선택으로 메타데이터를 추출하여 표시한다.
 *
 * @param viewModel ExtractViewModel 인스턴스
 * @param window    AWT Window (드래그앤드롭 DropTarget 등록용)
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun ExtractScreen(viewModel: ExtractViewModel, window: java.awt.Window) {
    val state by viewModel.state.collectAsState()
    val scrollState   = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text  = "메타데이터 추출",
            style = MaterialTheme.typography.headlineMedium,
            color = MockPackColors.OnBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text  = "APK 또는 IPA 파일을 드래그하거나 선택하여 메타데이터를 추출합니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MockPackColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        FileDropZone(
            window     = window,
            onFileDrop = { viewModel.onFileDrop(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val chooser = JFileChooser()
                    chooser.fileFilter = FileNameExtensionFilter("Mobile App Package", "apk", "ipa")
                    chooser.dialogTitle = "파일 선택"
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        viewModel.onFileDrop(chooser.selectedFile.toPath())
                    }
                }
            ) {
                Text("파일 선택")
            }

            if (state.metadata != null) {
                OutlinedButton(onClick = { viewModel.reset() }) {
                    Text("초기화")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("추출 중...", color = MockPackColors.TextSecondary)
                }
            }

            state.error != null -> {
                Text(
                    text  = "오류: ${state.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MockPackColors.Error
                )
            }

            state.metadata != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "추출 결과",
                        style = MaterialTheme.typography.titleMedium,
                        color = MockPackColors.OnBackground
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val jsonStr = viewModel.toJson() ?: return@launch
                                val chooser = JFileChooser()
                                chooser.dialogTitle    = "JSON 저장"
                                chooser.selectedFile   = File("metadata.json")
                                val result = chooser.showSaveDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    chooser.selectedFile.writeText(jsonStr)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MockPackColors.Primary
                        )
                    ) {
                        Text("JSON 내보내기")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                state.filePath?.let {
                    Text(
                        text  = "파일: ${it.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MockPackColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                MetadataTable(metadata = state.metadata!!)
            }
        }
    }
}
