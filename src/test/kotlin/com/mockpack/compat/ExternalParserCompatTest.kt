package com.mockpack.compat

import com.mockpack.build.ApkBuilder
import com.mockpack.core.model.AndroidMetadata
import net.dongliu.apk.parser.ApkFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * MockPack이 생성한 APK를 외부 라이브러리(net.dongliu:apk-parser)로
 * 파싱할 수 있는지 호환성을 검증하는 테스트.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ExternalParserCompatTest {

    @Test
    fun `외부 apk-parser로 Mock APK 메타데이터를 추출할 수 있다`(@TempDir tempDir: Path) {
        val original = AndroidMetadata(
            packageId        = "com.mockpack.compat",
            versionName      = "3.1.0",
            buildNumber      = "100",
            appName          = "CompatTest",
            minSdkVersion    = 24,
            targetSdkVersion = 34
        )

        val apkPath = tempDir.resolve("compat.apk")
        ApkBuilder().build(original, apkPath)

        ApkFile(apkPath.toFile()).use { apkParser ->
            val apkMeta = apkParser.apkMeta

            assertEquals("com.mockpack.compat", apkMeta.packageName)
            assertEquals("3.1.0", apkMeta.versionName)
            assertEquals(100L, apkMeta.versionCode)
            assertEquals("CompatTest", apkMeta.label)
            assertEquals("24", apkMeta.minSdkVersion)
            assertEquals("34", apkMeta.targetSdkVersion)
        }
    }
}
