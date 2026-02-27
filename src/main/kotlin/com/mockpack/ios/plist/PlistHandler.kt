package com.mockpack.ios.plist

import com.dd.plist.BinaryPropertyListWriter
import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.mockpack.core.error.ExtractionException
import com.mockpack.core.model.IosMetadata

/**
 * iOS plist 파일 파싱 및 생성을 담당하는 핸들러.
 * dd-plist 라이브러리를 래핑하여 바이너리/XML 양쪽 plist 형식을 모두 지원한다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object PlistHandler {

    /**
     * plist 바이트 데이터를 파싱하여 IosMetadata로 변환한다.
     * 바이너리 plist와 XML plist 모두 자동 감지하여 파싱한다.
     *
     * @param plistData Info.plist 파일의 바이트 배열
     * @returns 파싱된 IosMetadata
     * @throws ExtractionException plist 파싱 실패 또는 필수 키 누락 시
     */
    fun parseInfoPlist(plistData: ByteArray): IosMetadata {
        try {
            val rootObject = PropertyListParser.parse(plistData)

            if (rootObject !is NSDictionary) {
                throw ExtractionException("Info.plist 루트가 Dictionary가 아닙니다")
            }

            val bundleId = rootObject.getStringOrNull("CFBundleIdentifier")
                ?: throw ExtractionException("CFBundleIdentifier 키가 존재하지 않습니다")

            val versionName = rootObject.getStringOrNull("CFBundleShortVersionString")
                ?: rootObject.getStringOrNull("CFBundleVersion")
                ?: throw ExtractionException("CFBundleShortVersionString 키가 존재하지 않습니다")

            val buildNumber = rootObject.getStringOrNull("CFBundleVersion")
                ?: versionName

            val appName = rootObject.getStringOrNull("CFBundleDisplayName")
                ?: rootObject.getStringOrNull("CFBundleName")
                ?: rootObject.getStringOrNull("CFBundleExecutable")
                ?: "Unknown"

            val minimumOSVersion = rootObject.getStringOrNull("MinimumOSVersion") ?: ""

            val supportedPlatforms = rootObject.getStringListOrNull("CFBundleSupportedPlatforms")
                ?: emptyList()

            return IosMetadata(
                packageId          = bundleId,
                versionName        = versionName,
                buildNumber        = buildNumber,
                appName            = appName,
                minimumOSVersion   = minimumOSVersion,
                supportedPlatforms = supportedPlatforms
            )
        } catch (e: ExtractionException) {
            throw e
        } catch (e: Exception) {
            throw ExtractionException("Info.plist 파싱 실패", e)
        }
    }

    /**
     * IosMetadata를 바이너리 plist 형식의 바이트 배열로 변환한다.
     *
     * @param metadata 변환할 iOS 메타데이터
     * @returns 바이너리 plist 바이트 배열
     */
    fun createInfoPlist(metadata: IosMetadata): ByteArray {
        val dict = NSDictionary()

        dict["CFBundleIdentifier"]         = NSString(metadata.packageId)
        dict["CFBundleShortVersionString"] = NSString(metadata.versionName)
        dict["CFBundleVersion"]            = NSString(metadata.buildNumber)
        dict["CFBundleDisplayName"]        = NSString(metadata.appName)
        dict["CFBundleName"]               = NSString(metadata.appName)
        dict["CFBundleExecutable"]         = NSString(metadata.appName)
        dict["CFBundlePackageType"]        = NSString("APPL")
        dict["CFBundleInfoDictionaryVersion"] = NSString("6.0")

        if (metadata.minimumOSVersion.isNotBlank()) {
            dict["MinimumOSVersion"] = NSString(metadata.minimumOSVersion)
        }

        if (metadata.supportedPlatforms.isNotEmpty()) {
            val platforms = NSArray(metadata.supportedPlatforms.size)
            metadata.supportedPlatforms.forEachIndexed { index, platform ->
                platforms.setValue(index, NSString(platform))
            }
            dict["CFBundleSupportedPlatforms"] = platforms
        }

        dict["CFBundleDevelopmentRegion"]          = NSString("en")
        dict["CFBundleSignature"]                  = NSString("????")
        dict["LSRequiresIPhoneOS"]                 = NSNumber(true)
        dict["UIRequiredDeviceCapabilities"]        = NSArray(1).also {
            it.setValue(0, NSString("arm64"))
        }

        return BinaryPropertyListWriter.writeToArray(dict)
    }

    /**
     * NSDictionary에서 문자열 값을 안전하게 읽는다.
     */
    private fun NSDictionary.getStringOrNull(key: String): String? {
        val value = this[key] ?: return null
        return when (value) {
            is NSString -> value.content
            is NSNumber -> value.toString()
            else        -> value.toString()
        }
    }

    /**
     * NSDictionary에서 문자열 배열 값을 안전하게 읽는다.
     */
    private fun NSDictionary.getStringListOrNull(key: String): List<String>? {
        val value = this[key] ?: return null
        if (value !is NSArray) return null
        return value.array.map { it.toString() }
    }
}
