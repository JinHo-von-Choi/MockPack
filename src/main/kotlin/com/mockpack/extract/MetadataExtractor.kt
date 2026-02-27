package com.mockpack.extract

import com.mockpack.core.model.AppMetadata
import java.nio.file.Path

/**
 * 모바일 앱 패키지에서 메타데이터를 추출하는 인터페이스.
 * APK/IPA 각 플랫폼별 구현체가 이 인터페이스를 구현한다.
 *
 * @param T 추출 결과 메타데이터 타입 (AndroidMetadata 또는 IosMetadata)
 *
 * @author 최진호
 * @since 2026-02-27
 */
interface MetadataExtractor<T : AppMetadata> {

    /**
     * 지정된 파일에서 메타데이터를 추출한다.
     *
     * @param filePath 대상 파일 경로 (.apk 또는 .ipa)
     * @returns 추출된 메타데이터 객체
     * @throws com.mockpack.core.error.InvalidFileFormatException 파일 포맷이 올바르지 않은 경우
     * @throws com.mockpack.core.error.ExtractionException 추출 과정에서 오류 발생 시
     */
    fun extract(filePath: Path): T
}
