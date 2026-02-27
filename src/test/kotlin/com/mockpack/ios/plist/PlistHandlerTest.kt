package com.mockpack.ios.plist

import com.mockpack.core.model.IosMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * PlistHandler 단위 테스트.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class PlistHandlerTest {

    @Test
    fun `바이너리 plist 생성 후 파싱하면 원본 메타데이터와 일치한다`() {
        val original = IosMetadata(
            packageId        = "com.mockpack.test",
            versionName      = "2.1.0",
            buildNumber      = "456",
            appName          = "TestApp",
            minimumOSVersion = "16.0",
            supportedPlatforms = listOf("iPhoneOS")
        )

        val plistData = PlistHandler.createInfoPlist(original)
        val parsed    = PlistHandler.parseInfoPlist(plistData)

        assertEquals(original.packageId, parsed.packageId)
        assertEquals(original.versionName, parsed.versionName)
        assertEquals(original.buildNumber, parsed.buildNumber)
        assertEquals(original.appName, parsed.appName)
        assertEquals(original.minimumOSVersion, parsed.minimumOSVersion)
        assertEquals(original.supportedPlatforms, parsed.supportedPlatforms)
    }

    @Test
    fun `빈 바이트 배열을 파싱하면 ExtractionException이 발생한다`() {
        assertThrows<Exception> {
            PlistHandler.parseInfoPlist(ByteArray(0))
        }
    }
}
