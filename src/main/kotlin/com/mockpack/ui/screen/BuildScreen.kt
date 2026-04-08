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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mockpack.core.constant.Constants
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.core.model.IosMetadata
import com.mockpack.ui.component.FormField
import com.mockpack.ui.component.PlatformSelector
import com.mockpack.ui.theme.MockPackColors
import com.mockpack.ui.viewmodel.BuildViewModel
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Build 화면.
 * 메타데이터를 입력받아 Mock APK/IPA를 빌드한다.
 *
 * @param viewModel BuildViewModel 인스턴스
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun BuildScreen(viewModel: BuildViewModel) {
    val buildState by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var platform   by remember { mutableStateOf("android") }

    var packageId        by remember { mutableStateOf("") }
    var versionName      by remember { mutableStateOf(Constants.AndroidDefaults.VERSION_NAME) }
    var buildNumber      by remember { mutableStateOf(Constants.AndroidDefaults.VERSION_CODE) }
    var appName          by remember { mutableStateOf(Constants.AndroidDefaults.APP_NAME) }
    var minSdk           by remember { mutableStateOf(Constants.AndroidDefaults.MIN_SDK_VERSION.toString()) }
    var targetSdk        by remember { mutableStateOf(Constants.AndroidDefaults.TARGET_SDK_VERSION.toString()) }
    var minimumOSVersion by remember { mutableStateOf(Constants.IosDefaults.MINIMUM_OS_VERSION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text  = "Mock 빌드",
            style = MaterialTheme.typography.headlineMedium,
            color = MockPackColors.OnBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text  = "메타데이터를 입력하여 테스트용 Mock APK/IPA를 생성합니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MockPackColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        PlatformSelector(
            selectedPlatform = platform,
            onSelect         = { platform = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                label         = if (platform == "android") "Package Name" else "Bundle ID",
                value         = packageId,
                onValueChange = { packageId = it },
                placeholder   = "com.example.app"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    label         = "Version Name",
                    value         = versionName,
                    onValueChange = { versionName = it },
                    placeholder   = "1.0.0",
                    modifier      = Modifier.weight(1f)
                )
                FormField(
                    label         = if (platform == "android") "Version Code" else "Build Number",
                    value         = buildNumber,
                    onValueChange = { buildNumber = it },
                    placeholder   = "1",
                    modifier      = Modifier.weight(1f)
                )
            }

            FormField(
                label         = "App Name",
                value         = appName,
                onValueChange = { appName = it },
                placeholder   = "MockApp"
            )

            if (platform == "android") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormField(
                        label         = "Min SDK Version",
                        value         = minSdk,
                        onValueChange = { minSdk = it },
                        placeholder   = "21",
                        modifier      = Modifier.weight(1f)
                    )
                    FormField(
                        label         = "Target SDK Version",
                        value         = targetSdk,
                        onValueChange = { targetSdk = it },
                        placeholder   = "34",
                        modifier      = Modifier.weight(1f)
                    )
                }
            } else {
                FormField(
                    label         = "Minimum OS Version",
                    value         = minimumOSVersion,
                    onValueChange = { minimumOSVersion = it },
                    placeholder   = "15.0"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val prefs     = Preferences.userRoot().node("com/mockpack/app")
                val lastDir   = prefs.get("lastSaveDir", null)?.let { File(it) }

                val extension = if (platform == "android") "apk" else "ipa"
                val chooser   = JFileChooser(lastDir)
                chooser.dialogTitle  = "저장 위치 선택"
                chooser.selectedFile = File(lastDir, "${appName.ifBlank { "mock" }}.$extension")
                chooser.fileFilter   = FileNameExtensionFilter("${extension.uppercase()} File", extension)

                val result = chooser.showSaveDialog(null)
                if (result != JFileChooser.APPROVE_OPTION) return@Button

                var outputFile = chooser.selectedFile
                prefs.put("lastSaveDir", outputFile.parent)
                if (!outputFile.name.endsWith(".$extension")) {
                    outputFile = File("${outputFile.absolutePath}.$extension")
                }

                val metadata = if (platform == "android") {
                    AndroidMetadata(
                        packageId        = packageId,
                        versionName      = versionName,
                        buildNumber      = buildNumber,
                        appName          = appName,
                        minSdkVersion    = minSdk.toIntOrNull() ?: Constants.AndroidDefaults.MIN_SDK_VERSION,
                        targetSdkVersion = targetSdk.toIntOrNull() ?: Constants.AndroidDefaults.TARGET_SDK_VERSION
                    )
                } else {
                    IosMetadata(
                        packageId        = packageId,
                        versionName      = versionName,
                        buildNumber      = buildNumber,
                        appName          = appName,
                        minimumOSVersion = minimumOSVersion
                    )
                }

                viewModel.build(metadata, outputFile.toPath())
            },
            enabled  = packageId.isNotBlank() && !buildState.isBuilding,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MockPackColors.Primary)
        ) {
            if (buildState.isBuilding) {
                CircularProgressIndicator(color = MockPackColors.OnPrimary)
            } else {
                Text("빌드")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            buildState.error != null -> {
                Text(
                    text  = "오류: ${buildState.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MockPackColors.Error
                )
            }
            buildState.outputPath != null -> {
                Text(
                    text  = "빌드 완료: ${buildState.outputPath!!.fileName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MockPackColors.Success
                )
                Text(
                    text  = "${buildState.outputPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MockPackColors.TextSecondary
                )
            }
        }
    }
}
