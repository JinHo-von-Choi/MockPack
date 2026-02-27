package com.mockpack.extract

import com.mockpack.android.axml.AXMLWriter
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.util.ZipUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * ApkExtractor 단위 테스트.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ApkExtractorTest {

    @Test
    fun `AXML만 포함된 최소 APK에서 패키지명을 추출할 수 있다`(@TempDir tempDir: Path) {
        val metadata = AndroidMetadata(
            packageId        = "com.test.extract",
            versionName      = "1.2.3",
            buildNumber      = "10",
            appName          = "ExtractTest",
            minSdkVersion    = 21,
            targetSdkVersion = 33
        )

        val manifestData = AXMLWriter.generate(metadata)
        val apkPath      = tempDir.resolve("extract-test.apk")

        ZipUtils.createZip(apkPath, mapOf("AndroidManifest.xml" to manifestData))

        val extractor = ApkExtractor()
        val extracted = extractor.extract(apkPath)

        assertEquals("com.test.extract", extracted.packageId)
        assertEquals("1.2.3", extracted.versionName)
        assertEquals("10", extracted.buildNumber)
        assertEquals(21, extracted.minSdkVersion)
        assertEquals(33, extracted.targetSdkVersion)
    }
}
