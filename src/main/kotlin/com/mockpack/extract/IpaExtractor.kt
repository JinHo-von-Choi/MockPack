package com.mockpack.extract

import com.mockpack.core.constant.Constants
import com.mockpack.core.error.ExtractionException
import com.mockpack.core.error.InvalidFileFormatException
import com.mockpack.core.model.IosMetadata
import com.mockpack.ios.plist.PlistHandler
import com.mockpack.util.FileUtils
import com.mockpack.util.ZipUtils
import java.nio.file.Path

/**
 * IPA 파일에서 iOS 메타데이터를 추출하는 구현체.
 * IPA(ZIP) 내부의 Payload/XXX.app/Info.plist를 찾아 파싱한다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class IpaExtractor : MetadataExtractor<IosMetadata> {

    /**
     * IPA 파일에서 IosMetadata를 추출한다.
     *
     * IPA 내부 구조:
     * ```
     * Payload/
     *   AppName.app/
     *     Info.plist
     * ```
     *
     * @param filePath IPA 파일 경로
     * @returns 추출된 IosMetadata
     * @throws InvalidFileFormatException IPA 구조가 올바르지 않은 경우
     * @throws ExtractionException plist 파싱 실패 시
     */
    override fun extract(filePath: Path): IosMetadata {
        FileUtils.validateFileExists(filePath)

        val plistEntry = ZipUtils.findAndReadEntry(filePath) { entryName ->
            entryName.startsWith(Constants.IPA_PAYLOAD_PREFIX)
                && entryName.endsWith("/${Constants.INFO_PLIST}")
                && entryName.count { it == '/' } == 2
        } ?: throw InvalidFileFormatException(
            "IPA 내에서 Info.plist를 찾을 수 없음: ${filePath.fileName}"
        )

        val (entryName, plistData) = plistEntry

        try {
            return PlistHandler.parseInfoPlist(plistData)
        } catch (e: ExtractionException) {
            throw e
        } catch (e: Exception) {
            throw ExtractionException("Info.plist 파싱 실패 ($entryName)", e)
        }
    }
}
