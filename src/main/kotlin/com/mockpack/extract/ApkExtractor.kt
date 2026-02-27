package com.mockpack.extract

import com.mockpack.android.axml.AXMLConstants
import com.mockpack.android.axml.AXMLParser
import com.mockpack.core.constant.Constants
import com.mockpack.core.error.ExtractionException
import com.mockpack.core.error.InvalidFileFormatException
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.util.FileUtils
import com.mockpack.util.ZipUtils
import java.nio.file.Path

/**
 * APK 파일에서 Android 메타데이터를 추출하는 구현체.
 * AndroidManifest.xml(AXML)을 파싱하여 패키지명, 버전, SDK 정보를 추출한다.
 * app_name은 resources.arsc에서 가져오거나, manifest의 label 속성에서 읽는다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ApkExtractor : MetadataExtractor<AndroidMetadata> {

    /**
     * APK 파일에서 AndroidMetadata를 추출한다.
     *
     * @param filePath APK 파일 경로
     * @returns 추출된 AndroidMetadata
     * @throws InvalidFileFormatException APK 구조가 올바르지 않은 경우
     * @throws ExtractionException AXML 파싱 실패 시
     */
    override fun extract(filePath: Path): AndroidMetadata {
        FileUtils.validateFileExists(filePath)

        val manifestData = ZipUtils.readEntry(filePath, Constants.ANDROID_MANIFEST_ENTRY)

        val parser = AXMLParser(manifestData)
        parser.parse()

        val packageName = parser.findAttributeValue("manifest", "package")
            ?: throw ExtractionException("manifest 태그에 package 속성이 없습니다")

        val versionName = parser.findAttributeValue(
            "manifest", "versionName", AXMLConstants.ANDROID_NS_URI
        ) ?: "unknown"

        val versionCode = parser.findAttributeIntValue(
            "manifest", "versionCode", AXMLConstants.ANDROID_NS_URI
        )?.toString() ?: "0"

        val minSdk = parser.findAttributeIntValue(
            "uses-sdk", "minSdkVersion", AXMLConstants.ANDROID_NS_URI
        ) ?: 1

        val targetSdk = parser.findAttributeIntValue(
            "uses-sdk", "targetSdkVersion", AXMLConstants.ANDROID_NS_URI
        ) ?: minSdk

        val appName = resolveAppName(filePath, parser)

        return AndroidMetadata(
            packageId        = packageName,
            versionName      = versionName,
            buildNumber      = versionCode,
            appName          = appName,
            minSdkVersion    = minSdk,
            targetSdkVersion = targetSdk
        )
    }

    /**
     * application 태그의 label 속성에서 앱 이름을 추출한다.
     * 문자열이면 직접 사용하고, 리소스 참조면 resources.arsc에서 검색한다.
     */
    private fun resolveAppName(filePath: Path, parser: AXMLParser): String {
        val labelValue = parser.findAttributeValue(
            "application", "label", AXMLConstants.ANDROID_NS_URI
        )

        if (labelValue == null) return "Unknown"

        if (!labelValue.startsWith("@0x")) {
            return labelValue
        }

        try {
            val resourceId = labelValue.removePrefix("@").toInt(16)

            if (ZipUtils.hasEntry(filePath, Constants.RESOURCES_ARSC_ENTRY)) {
                val arscData = ZipUtils.readEntry(filePath, Constants.RESOURCES_ARSC_ENTRY)
                val resolved = com.mockpack.android.resource.ResourceTableParser
                    .findStringByResourceId(arscData, resourceId)
                if (resolved != null) return resolved
            }
        } catch (_: Exception) {
            /** 리소스 해석 실패 시 원본 참조 문자열 반환 */
        }

        return labelValue
    }
}
