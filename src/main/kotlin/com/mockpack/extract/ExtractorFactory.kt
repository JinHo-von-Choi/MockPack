package com.mockpack.extract

import com.mockpack.core.constant.Constants
import com.mockpack.core.error.InvalidFileFormatException
import com.mockpack.core.model.AppMetadata
import java.nio.file.Path

/**
 * 파일 확장자를 기반으로 적절한 MetadataExtractor 구현체를 생성하는 팩토리.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ExtractorFactory {

    /**
     * 파일 경로의 확장자를 분석하여 해당 플랫폼의 추출기를 반환한다.
     *
     * @param filePath 대상 파일 경로
     * @returns 플랫폼에 맞는 MetadataExtractor 구현체
     * @throws InvalidFileFormatException 지원하지 않는 파일 확장자인 경우
     */
    fun create(filePath: Path): MetadataExtractor<out AppMetadata> {
        val fileName = filePath.fileName.toString().lowercase()

        return when {
            fileName.endsWith(Constants.APK_EXTENSION) -> ApkExtractor()
            fileName.endsWith(Constants.IPA_EXTENSION) -> IpaExtractor()
            else -> throw InvalidFileFormatException(
                "지원하지 않는 파일 형식: $fileName (APK 또는 IPA 파일만 지원)"
            )
        }
    }
}
